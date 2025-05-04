package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubCommitDetailService {

    private final WebClient webClient;
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;

    /**
     * 선택된 커밋의 상세 정보를 GitHub API를 통해 조회합니다.
     */
    public CommitDetailResponseDTO.CommitDetailResponse getCommitDetails(
            CommitDetailRequestDTO.CommitDetailRequest request, Long userId) {

        // 시작 시간 기록
        long startTime = System.currentTimeMillis();
        log.info("선택된 커밋 상세 조회 시작: {}개 커밋, 레포지토리ID={}, 브랜치={}",
                request.getCommits().size(), request.getRepositoryId(), request.getBranch());

        // 사용자 조회 및 토큰 유효성 검사
        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);
        String token = decryptToken(user.getGithubToken());

        // 사용자의 GitHub 사용자명 가져오기
        String authorUsername = getUsernameFromToken(token);

        // 레포지토리 정보 조회
        String owner;
        String repoName;

        try {
            if (request.getOrganizationId() != null) {
                owner = getOrganizationLogin(request.getOrganizationId(), token);
                repoName = getRepositoryNameFromOrg(owner, request.getRepositoryId(), token);
            } else {
                Map.Entry<String, String> result = getPersonalRepoInfo(request.getRepositoryId(), token);
                owner = result.getKey();
                repoName = result.getValue();
            }

            log.info("레포지토리 정보 조회 완료: 소유자={}, 레포={}", owner, repoName);
        } catch (Exception e) {
            log.error("레포지토리 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("레포지토리 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 커밋 상세 정보 조회
        List<CommitDetailResponseDTO.CommitDetail> commitDetails = new ArrayList<>();
        for (CommitDetailRequestDTO.CommitSummary commitSummary : request.getCommits()) {
            try {
                // 1. 커밋 기본 정보 가져오기
                Map<String, Object> commitInfo = fetchCommitBasicInfo(
                        owner,
                        repoName,
                        commitSummary.getSha(),
                        token
                );

                // commitInfo가 null이면 다음 커밋으로 계속 진행
                if (commitInfo == null) {
                    log.warn("커밋을 찾을 수 없음: sha={}", commitSummary.getSha());
                    continue;
                }

                // 자신이 작성한 커밋인지 확인
                Map<String, Object> apiAuthor = (Map<String, Object>) commitInfo.get("author");
                if (apiAuthor != null && !authorUsername.equals(apiAuthor.get("login"))) {
                    log.info("본인이 작성한 커밋이 아님: sha={}, author={}", commitSummary.getSha(), apiAuthor.get("login"));
                    continue;
                }

                // 2. 커밋 내 파일 변경 정보 가져오기
                List<CommitDetailResponseDTO.FileDetail> fileDetails = fetchFileChanges(
                        commitInfo,
                        owner,
                        repoName,
                        request.getBranch(),
                        token
                );

                // 3. 커밋 상세 정보 조합
                CommitDetailResponseDTO.CommitDetail commitDetail = buildCommitDetail(
                        commitSummary.getSha(),
                        commitSummary.getMessage(),
                        commitInfo,
                        fileDetails
                );

                commitDetails.add(commitDetail);
                log.info("커밋 상세 정보 조회 완료: sha={}", commitSummary.getSha());
            } catch (WebClientResponseException e) {
                log.error("GitHub API 호출 실패: {} - {}, SHA: {}",
                        e.getStatusCode(), e.getMessage(), commitSummary.getSha());
                // 오류가 발생하더라도 다음 커밋 처리를 위해 계속 진행
            } catch (Exception e) {
                log.error("커밋 상세 정보 조회 실패 (sha={}): {}", commitSummary.getSha(), e.getMessage());
                // 오류가 발생하더라도 다음 커밋 처리를 위해 계속 진행
            }
        }

        // 종료 시간 기록 및 소요 시간 계산
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        log.info("선택된 커밋 상세 조회 완료: {}개 커밋, 소요 시간: {}ms", commitDetails.size(), duration);

        // 응답 조합
        return CommitDetailResponseDTO.CommitDetailResponse.builder()
                .repoOwner(owner)
                .repo(repoName)
                .branch(request.getBranch())
                .commits(commitDetails)
                .build();
    }

    // GithubCommitDetailService 클래스에 없을 경우 추가 필요
    private String getUsernameFromToken(String token) {
        Map<String, Object> userInfo = webClient.get()
                .uri("https://api.github.com/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().bodyToMono(Map.class).block();

        return userInfo != null ? userInfo.get("login").toString() : "unknown";
    }

    /**
     * 커밋의 기본 정보를 가져옵니다.
     */
    private Map<String, Object> fetchCommitBasicInfo(String owner, String repo, String sha, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, sha);
        log.info("GitHub API 호출: 커밋 기본 정보 조회 - {}", url);

        try {
            Map<String, Object> commitInfo = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            log.info("커밋 기본 정보 조회 성공: sha={}", sha);
            return commitInfo;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 422 || e.getStatusCode().value() == 404) {
                log.warn("유효하지 않은 커밋 SHA: {}, 상태 코드: {}", sha, e.getStatusCode());
                return null;
            }
            throw e;
        } catch (Exception e) {
            log.error("커밋 기본 정보 조회 오류: {}", e.getMessage());
            throw new RuntimeException("커밋 기본 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 커밋에 변경된 파일 정보를 가져오고, 각 파일의 최신 코드도 조회합니다.
     */
    private List<CommitDetailResponseDTO.FileDetail> fetchFileChanges(
            Map<String, Object> commitInfo, String owner, String repo, String branch, String token) {

        List<Map<String, Object>> files = (List<Map<String, Object>>) commitInfo.get("files");
        if (files == null || files.isEmpty()) {
            log.info("변경된 파일 없음");
            return Collections.emptyList();
        }

        List<CommitDetailResponseDTO.FileDetail> fileDetails = new ArrayList<>();
        for (Map<String, Object> file : files) {
            String filepath = file.get("filename").toString();
            String patch = file.containsKey("patch") ? file.get("patch").toString() : "";
            String status = file.get("status").toString(); // 필요한 상태 정보만 내부적으로 사용

            // 파일의 최신 코드 조회 (status가 'removed'가 아닌 경우에만)
            String latestCode = "";
            if (!"removed".equals(status)) {
                try {
                    latestCode = fetchFileContent(owner, repo, filepath, branch, token);
                } catch (Exception e) {
                    log.warn("파일 내용 조회 실패: {}, 오류: {}", filepath, e.getMessage());
                    // 파일 내용 조회 실패 시 빈 문자열로 처리하고 계속 진행
                }
            }

            CommitDetailResponseDTO.FileDetail fileDetail = CommitDetailResponseDTO.FileDetail.builder()
                    .filepath(filepath)
                    .patch(patch)
                    .latestCode(latestCode)
                    .build();

            fileDetails.add(fileDetail);
            log.debug("파일 상세 정보 조회 완료: {}", filepath);
        }

        return fileDetails;
    }

    /**
     * 특정 파일의 최신 내용을 가져옵니다.
     */
    private String fetchFileContent(String owner, String repo, String path, String branch, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                owner, repo, path, branch);
        log.debug("GitHub API 호출: 파일 내용 조회 - {}", url);

        try {
            Map<String, Object> fileInfo = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (fileInfo == null) {
                return "";
            }

            if (fileInfo.containsKey("content")) {
                String encodedContent = fileInfo.get("content").toString();
                // Base64로 인코딩된 내용 디코딩
                String cleanedContent = encodedContent.replace("\n", "");
                return new String(Base64.getDecoder().decode(cleanedContent));
            } else if (fileInfo.containsKey("download_url")) {
                // download_url로 직접 파일 내용 가져오기
                String downloadUrl = fileInfo.get("download_url").toString();
                return webClient.get()
                        .uri(downloadUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
            }

            return "";
        } catch (Exception e) {
            log.warn("파일 내용 조회 오류: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 커밋 상세 정보를 구성합니다.
     */
    private CommitDetailResponseDTO.CommitDetail buildCommitDetail(
            String sha, String message, Map<String, Object> commitInfo,
            List<CommitDetailResponseDTO.FileDetail> fileDetails) {

        Map<String, Object> commit = (Map<String, Object>) commitInfo.get("commit");
        Map<String, Object> author = (Map<String, Object>) commit.get("author");

        String authorName = author.get("name").toString();
        String authorEmail = author.get("email").toString();
        String authorDate = author.get("date").toString();

        return CommitDetailResponseDTO.CommitDetail.builder()
                .sha(sha)
                .message(message)
                .authorName(authorName)
                .authorEmail(authorEmail)
                .authorDate(authorDate)
                .files(fileDetails)
                .build();
    }

    private String getOrganizationLogin(Long organizationId, String token) {
        Map<String, Object>[] orgs = webClient.get()
                .uri("https://api.github.com/user/orgs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        if (orgs == null || orgs.length == 0) {
            throw new RuntimeException("사용자의 조직 정보를 찾을 수 없습니다.");
        }

        for (Map<String, Object> org : orgs) {
            if (Long.valueOf(org.get("id").toString()).equals(organizationId)) {
                return org.get("login").toString();
            }
        }

        throw new RuntimeException("해당 조직을 찾을 수 없습니다 (ID: " + organizationId + ")");
    }

    private String getRepositoryNameFromOrg(String owner, Long repositoryId, String token) {
        Map<String, Object>[] repos = webClient.get()
                .uri("https://api.github.com/orgs/" + owner + "/repos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        if (repos == null || repos.length == 0) {
            throw new RuntimeException("조직의 레포지토리 정보를 찾을 수 없습니다.");
        }

        for (Map<String, Object> repo : repos) {
            if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                return repo.get("name").toString();
            }
        }

        throw new RuntimeException("조직 내에서 레포지토리를 찾을 수 없습니다 (ID: " + repositoryId + ")");
    }

    private Map.Entry<String, String> getPersonalRepoInfo(Long repositoryId, String token) {
        Map<String, Object>[] repos = webClient.get()
                .uri("https://api.github.com/user/repos?affiliation=owner,collaborator")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        if (repos == null || repos.length == 0) {
            throw new RuntimeException("사용자의 레포지토리 정보를 찾을 수 없습니다.");
        }

        for (Map<String, Object> repo : repos) {
            if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                String repoName = repo.get("name").toString();
                String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
                return Map.entry(owner, repoName);
            }
        }

        throw new RuntimeException("해당 레포지토리를 찾을 수 없습니다 (ID: " + repositoryId + ")");
    }

    /**
     * 사용자의 GitHub 토큰이 있는지 확인합니다.
     */
    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new RuntimeException("GitHub 토큰이 없습니다.");
        }
    }

    /**
     * 암호화된 GitHub 토큰을 복호화합니다.
     */
    private String decryptToken(String token) {
        try {
            return tokenEncryptor.decrypt(token);
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰 복호화 실패: " + e.getMessage());
        }
    }
}
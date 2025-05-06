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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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
        String username = getUsernameFromToken(token);

        // 현재 날짜 포맷 (기본값으로 사용)
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        // 실제 커밋 날짜를 저장할 변수 (기본값은 현재 날짜)
        String commitDate = currentDate;

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

        // 파일별로 패치 정보 그룹화 준비
        Map<String, List<CommitDetailResponseDTO.PatchDetail>> filePatches = new HashMap<>();
        Map<String, String> fileContents = new HashMap<>();

        // 커밋 처리
        for (CommitDetailRequestDTO.CommitSummary commitSummary : request.getCommits()) {
            try {
                // 커밋 기본 정보 가져오기
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

                // 커밋 날짜 추출 (첫 번째 유효한 커밋에서 추출)
                if (commitDate.equals(currentDate)) {
                    try {
                        Map<String, Object> commit = (Map<String, Object>) commitInfo.get("commit");
                        if (commit != null && commit.containsKey("committer")) {
                            Map<String, Object> committer = (Map<String, Object>) commit.get("committer");
                            if (committer != null && committer.containsKey("date")) {
                                String dateStr = committer.get("date").toString();

                                // GitHub API의 날짜 형식은 ISO 8601 (예: 2024-05-06T10:30:00Z)
                                // 이를 YYYY-MM-DD 형식으로 변환
                                OffsetDateTime dateTime = OffsetDateTime.parse(dateStr);
                                commitDate = dateTime.toLocalDate().toString();
                                log.info("커밋 날짜 추출: {}", commitDate);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("커밋 날짜 추출 오류: {}", e.getMessage());
                        // 날짜 파싱 실패시 현재 날짜를 사용
                    }
                }

                // 자신이 작성한 커밋인지 확인
                Map<String, Object> apiAuthor = (Map<String, Object>) commitInfo.get("author");
                if (apiAuthor != null && !username.equals(apiAuthor.get("login"))) {
                    log.info("본인이 작성한 커밋이 아님: sha={}, author={}", commitSummary.getSha(), apiAuthor.get("login"));
                    continue;
                }

                // 파일 변경 정보 처리
                List<Map<String, Object>> files = (List<Map<String, Object>>) commitInfo.get("files");
                if (files != null && !files.isEmpty()) {
                    for (Map<String, Object> file : files) {
                        String filepath = file.get("filename").toString();
                        String patch = file.containsKey("patch") ? file.get("patch").toString() : "";
                        String status = file.get("status").toString();

                        // 파일 내용 조회 (최신 코드)
                        if (!fileContents.containsKey(filepath) && !"removed".equals(status)) {
                            try {
                                String latestCode = fetchFileContent(owner, repoName, filepath, request.getBranch(), token);
                                fileContents.put(filepath, latestCode);
                            } catch (Exception e) {
                                log.warn("파일 내용 조회 실패: {}, 오류: {}", filepath, e.getMessage());
                                fileContents.put(filepath, "");
                            }
                        }

                        // 패치 정보 생성
                        CommitDetailResponseDTO.PatchDetail patchDetail = CommitDetailResponseDTO.PatchDetail.builder()
                                .commit_message(commitSummary.getMessage())
                                .patch(patch)
                                .build();

                        // 파일별 패치 정보 그룹화
                        if (!filePatches.containsKey(filepath)) {
                            filePatches.put(filepath, new ArrayList<>());
                        }
                        filePatches.get(filepath).add(patchDetail);
                    }
                }

                log.info("커밋 정보 처리 완료: sha={}, 메시지={}", commitSummary.getSha(), commitSummary.getMessage());
            } catch (WebClientResponseException e) {
                log.error("GitHub API 호출 실패: {} - {}, SHA: {}",
                        e.getStatusCode(), e.getMessage(), commitSummary.getSha());
                // 오류가 발생하더라도 다음 커밋 처리를 위해 계속 진행
            } catch (Exception e) {
                log.error("커밋 상세 정보 조회 실패 (sha={}): {}", commitSummary.getSha(), e.getMessage());
                // 오류가 발생하더라도 다음 커밋 처리를 위해 계속 진행
            }
        }

        // 파일 상세 정보 리스트 생성
        List<CommitDetailResponseDTO.FileDetail> fileDetails = new ArrayList<>();
        for (String filepath : filePatches.keySet()) {
            CommitDetailResponseDTO.FileDetail fileDetail = CommitDetailResponseDTO.FileDetail.builder()
                    .filepath(filepath)
                    .latest_code(fileContents.getOrDefault(filepath, ""))
                    .patches(filePatches.get(filepath))
                    .build();
            fileDetails.add(fileDetail);
        }

        // 종료 시간 기록 및 소요 시간 계산
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        log.info("선택된 커밋 상세 조회 완료: {}개 파일, 소요 시간: {}ms", fileDetails.size(), duration);

        // 최종 응답 생성 - branch 필드 제거, 실제 커밋 날짜 사용
        return CommitDetailResponseDTO.CommitDetailResponse.builder()
                .username(username)
                .date(commitDate)
                .repo(repoName)
                .files(fileDetails)
                .build();
    }

    // 나머지 메소드들은 그대로 사용
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
            //Todo :
            log.warn("파일 내용 조회 오류: {}", e.getMessage());
            return "";
        }
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
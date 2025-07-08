package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubCommitDetailService {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

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
            Map<String, Object> repoMeta = getRepositoryById(request.getRepositoryId(), token);
            owner = ((Map<String, Object>) repoMeta.get("owner")).get("login").toString();
            repoName = (String) repoMeta.get("name");
            log.info("레포지토리 정보 조회 완료: 소유자={}, 레포={}", owner, repoName);
        } catch (Exception e) {
            log.error("레포지토리 메타데이터 조회 실패: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage() + ": " + e.getMessage());
        }

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

                if (commitInfo == null) {
                    log.warn("커밋을 찾을 수 없음: sha={}", commitSummary.getSha());
                    continue;
                }

                // 커밋 날짜 추출 (첫 번째 유효한 커밋에서 추출) - KST 기준
                if (commitDate.equals(currentDate)) {
                    commitDate = extractKstCommitDate(commitInfo);
                }

                // 자신이 작성한 커밋인지 확인
                Map<String, Object> apiAuthor = (Map<String, Object>) commitInfo.get("author");
                if (apiAuthor != null && !username.equals(apiAuthor.get("login"))) {
                    log.info("본인이 작성한 커밋이 아님: sha={}", commitSummary.getSha());
                    continue;
                }

                // 파일 변경 정보 처리
                List<Map<String, Object>> files = (List<Map<String, Object>>) commitInfo.get("files");
                if (files != null && !files.isEmpty()) {
                    for (Map<String, Object> file : files) {
                        String filepath = file.get("filename").toString();
                        String patch = file.containsKey("patch") ? file.get("patch").toString() : "";
                        String status = file.get("status").toString();

                        // 파일 내용 조회
                        if (!fileContents.containsKey(filepath) && !"removed".equals(status)) {
                            try {
                                String latestCode = fetchFileContent(owner, repoName, filepath,
                                        commitSummary.getSha(), token);
                                fileContents.put(filepath, latestCode);
                            } catch (Exception e) {
                                log.warn("커밋 시점 파일 내용 조회 실패: {}, 오류: {}", filepath, e.getMessage());
                                fileContents.put(filepath, "");
                            }
                        }

                        CommitDetailResponseDTO.PatchDetail patchDetail =
                                CommitDetailResponseDTO.PatchDetail.builder()
                                        .commit_message(commitSummary.getMessage())
                                        .patch(patch)
                                        .build();

                        filePatches.computeIfAbsent(filepath, k -> new ArrayList<>()).add(patchDetail);
                    }
                }

                log.info("커밋 정보 처리 완료: sha={}, 메시지={}", commitSummary.getSha(),
                        commitSummary.getMessage());
            } catch (WebClientResponseException e) {
                log.error("GitHub API 호출 실패: {} - {}, SHA: {}",
                        e.getStatusCode(), e.getMessage(), commitSummary.getSha());
                // 오류가 발생하더라도 다음 커밋 처리를 위해 계속 진행
            } catch (Exception e) {
                log.error("커밋 상세 정보 조회 실패 (sha={}): {}", commitSummary.getSha(), e.getMessage());
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

        long duration = System.currentTimeMillis() - startTime;
        log.info("선택된 커밋 상세 조회 완료: {}개 파일, 소요시간: {}ms", fileDetails.size(), duration);

        return GitHubDtoConverter.toCommitDetailResponse(fileDetails, username, commitDate, repoName);
    }

    /**
     * 커밋 정보에서 KST 날짜 추출
     */
    private String extractKstCommitDate(Map<String, Object> commitInfo) {
        try {
            Map<String, Object> commit = (Map<String, Object>) commitInfo.get("commit");
            if (commit != null && commit.containsKey("committer")) {
                Map<String, Object> committer = (Map<String, Object>) commit.get("committer");
                if (committer != null && committer.containsKey("date")) {
                    String dateStr = committer.get("date").toString();

                    // UTC -> KST 변환
                    Instant instant = Instant.parse(dateStr);
                    String kstDate = instant.atZone(KST_ZONE).toLocalDate().toString();
                    return kstDate;
                }
            }
        } catch (Exception e) {
            log.warn("커밋 날짜 추출 오류: {}", e.getMessage());
        }
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String getUsernameFromToken(String token) {
        try {
            Map<String, Object> userInfo = webClient.get()
                    .uri("https://api.github.com/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().bodyToMono(Map.class).block();

            if (userInfo != null && userInfo.get("login") != null) {
                return userInfo.get("login").toString();
            }
            return "unknown";
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return "unknown";
        }
    }

    private Map<String, Object> fetchCommitBasicInfo(String owner, String repo, String sha, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/commits/%s", owner, repo, sha);

        try {
            return webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 422 || e.getStatusCode().value() == 404) {
                log.warn("유효하지 않은 커밋 SHA: {}", sha);
                return null;
            }
            throw e;
        }
    }

    private String fetchFileContent(String owner, String repo, String path, String ref, String token) {
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                owner, repo, path, ref);

        try {
            Map<String, Object> fileInfo = webClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (fileInfo == null) return "";

            if (fileInfo.containsKey("content")) {
                String encodedContent = fileInfo.get("content").toString();
                String cleanedContent = encodedContent.replace("\n", "");
                return new String(Base64.getDecoder().decode(cleanedContent));
            } else if (fileInfo.containsKey("download_url")) {
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

    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_MISSING.getMessage());
        }
    }

    private String decryptToken(String token) {
        try {
            return tokenEncryptor.decrypt(token);
        } catch (Exception e) {
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_DECRYPT_ERROR.getMessage());
        }
    }

    private Map<String, Object> getRepositoryById(Long repositoryId, String token) {
        try {
            return webClient.get()
                    .uri("https://api.github.com/repositories/" + repositoryId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("레포지토리 조회 실패: ID={}", repositoryId);
            throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage());
        }
    }
}

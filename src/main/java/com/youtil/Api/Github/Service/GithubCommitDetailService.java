package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Constants.GitHubApiConstants;
import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Util.GitHubCacheHelper;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import org.springframework.util.DigestUtils;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import static com.youtil.Api.Github.Constants.GithubCacheConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubCommitDetailService {

    private final WebClient webClient;
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;
    private final GitHubCacheHelper cacheHelper;
    private final GitHubApiUtils gitHubApiUtils;

    /**
     * 선택된 커밋의 상세 정보를 GitHub API를 통해 조회
     * 캐시 우선 조회 > APi 호출
     */
    public CommitDetailResponseDTO.CommitDetailResponse getCommitDetails(
            CommitDetailRequestDTO.CommitDetailRequest request, Long userId) {

        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        String cacheKey = buildCommitDetailCacheKey(userId, request);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "commit_detail",
                CommitDetailResponseDTO.CommitDetailResponse.class,
                () -> {
                    CommitDetailResponseDTO.CommitDetailResponse response = fetchCommitDetailsFromGithub(request, user);
                    cacheHelper.saveToCache(cacheKey, response, COMMIT_DETAIL_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 커밋 상세 캐시 키 생성
     */
    private String buildCommitDetailCacheKey(Long userId, CommitDetailRequestDTO.CommitDetailRequest request) {
        // 커밋 SHA들을 정렬해서 일관된 키 생성
        List<String> shas = request.getCommits().stream()
                .map(CommitDetailRequestDTO.CommitSummary::getSha)
                .sorted()
                .collect(Collectors.toList());

        String shasCombined = String.join(",", shas);
        // MD5 해시로 안전한 키 생성 (SHA-256 대신 MD5 사용 - 더 짧고 충돌 확률 낮음)
        String hashedShas = DigestUtils.md5DigestAsHex(shasCombined.getBytes());

        return String.format("%s%d:repo:%d:branch:%s:shas:%s",
                COMMIT_DETAIL_CACHE_KEY, userId, request.getRepositoryId(),
                request.getBranch(), hashedShas);
    }

    /**
     * GitHub API 호출하여 각 커밋의 상세 정보 조회
     */
    private CommitDetailResponseDTO.CommitDetailResponse fetchCommitDetailsFromGithub(
            CommitDetailRequestDTO.CommitDetailRequest request, User user) {

        // 시작 시간 기록
        long startTime = System.currentTimeMillis();
        log.info("선택된 커밋 상세 조회 시작: {}개 커밋, 레포지토리ID={}, 브랜치={}",
                request.getCommits().size(), request.getRepositoryId(), request.getBranch());

        String token = gitHubApiUtils.decryptToken(user.getGithubToken());
        String username = gitHubApiUtils.getUsernameFromToken(token);

        // 현재 날짜 포맷 (기본값으로 사용)
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String commitDate = currentDate;

        // 레포지토리 정보 조회
        String owner;
        String repoName;

        try {
            Map<String, Object> repoMeta = gitHubApiUtils.getRepositoryById(request.getRepositoryId(), token);
            owner = ((Map<String, Object>) repoMeta.get("owner")).get("login").toString();
            repoName = (String) repoMeta.get("name");
            log.info("레포지토리 정보 조회 완료: 소유자={}, 레포={}", owner, repoName);
        } catch (Exception e) {
            log.error("레포지토리 메타데이터 조회 실패: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage() + ": " + e.getMessage());
        }

        // 파일별로 패치 정보 그룹화 준비
        Map<String, List<CommitDetailResponseDTO.PatchDetail>> filePatches = new HashMap<>();
        Map<String, String> fileContents = new HashMap<>();

        // 커밋 처리
        for (CommitDetailRequestDTO.CommitSummary commitSummary : request.getCommits()) {
            try {
                // 커밋 기본 정보 가져오기
                Map<String, Object> commitInfo = fetchCommitBasicInfo(owner, repoName, commitSummary.getSha(), token);

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
                                OffsetDateTime dateTime = OffsetDateTime.parse(dateStr);
                                commitDate = dateTime.toLocalDate().toString();
                                log.info("커밋 날짜 추출: {}", commitDate);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("커밋 날짜 추출 오류: {}", e.getMessage());
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

                        // 파일 내용 조회 (커밋 시점의 코드)
                        if (!fileContents.containsKey(filepath) && !"removed".equals(status)) {
                            try {
                                String latestCode = fetchFileContent(owner, repoName, filepath, commitSummary.getSha(), token);
                                fileContents.put(filepath, latestCode);
                            } catch (Exception e) {
                                log.warn("커밋 시점 파일 내용 조회 실패: {}, 오류: {}", filepath, e.getMessage());
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
                log.error("GitHub API 호출 실패: {} - {}, SHA: {}", e.getStatusCode(), e.getMessage(), commitSummary.getSha());
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

        // 종료 시간 기록 및 소요 시간 계산
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        log.info("선택된 커밋 상세 조회 완료: {}개 파일, 소요 시간: {}ms", fileDetails.size(), duration);

        // 최종 응답 생성 - GitHubDtoConverter 활용
        return GitHubDtoConverter.toCommitDetailResponse(fileDetails, username, commitDate, repoName);
    }


    /**
     * 개별 커밋의 기본 정보 (메시지, 파일 변경 목록) 조회
     */
    private Map<String, Object> fetchCommitBasicInfo(String owner, String repo, String sha, String token) {
        String url = GitHubApiConstants.REPOS_BASE_URL + owner + "/" + repo + GitHubApiConstants.COMMITS_PATH + "/" + sha;
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
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        }
    }

    /**
     * 커밋 시점의 파일 내용 조회
     */
    private String fetchFileContent(String owner, String repo, String path, String ref, String token) {
        String url = GitHubApiConstants.REPOS_BASE_URL + owner + "/" + repo + GitHubApiConstants.CONTENTS_PATH + path
                + "?ref=" + ref;
        log.debug("GitHub API 호출: 커밋 시점 파일 내용 조회 - {}", url);


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
            log.warn("커밋 시점 파일 내용 조회 오류: {}", e.getMessage());
            return "";
        }
    }
}

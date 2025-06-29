package com.youtil.Api.Github.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Github.Constants.GitHubApiConstants;
import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Api.Github.Util.GitHubCacheHelper;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.youtil.Api.Github.Constants.GithubCacheConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubCommitSummaryService {

    private static final DateTimeFormatter GITHUB_COMMIT_DATE_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final WebClient webClient;
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;
    private final GitHubCacheHelper cacheHelper;
    private final GitHubApiUtils gitHubApiUtils;

    /**
     * 특정 날짜의 커밋 요약 정보(SHA, 메시지)만 조회
     */
    public CommitSummaryResponseDTO.CommitSummaryResponse getCommitSummary(Long userId,
                                                                           Long organizationId,
                                                                           Long repositoryId, String branch, String date,
                                                                           Integer page, Integer offset) {

        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        String cacheKey = buildCommitSummaryCacheKey(userId, repositoryId, branch, date, page, offset);

        return cacheHelper.getFromCacheWithFallback(
                cacheKey,
                "commit_summary",
                CommitSummaryResponseDTO.CommitSummaryResponse.class,
                () -> {
                    CommitSummaryResponseDTO.CommitSummaryResponse response = fetchCommitSummaryFromGithub(
                            user, organizationId, repositoryId, branch, date, page, offset);
                    cacheHelper.saveToCache(cacheKey, response, COMMIT_CACHE_TTL);
                    return response;
                }
        );
    }

    /**
     * 커밋 요약 캐시 키 생성
     */
    private String buildCommitSummaryCacheKey(Long userId, Long repositoryId, String branch, String date, Integer page, Integer offset) {
        return String.format("%s%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                COMMIT_CACHE_KEY, userId, repositoryId, branch, date, page, offset);
    }

    /**
     * GitHub API 호출 전 날짜 파싱, 레포 정보 조회
     */
    private CommitSummaryResponseDTO.CommitSummaryResponse fetchCommitSummaryFromGithub(
            User user, Long organizationId, Long repositoryId, String branch, String date, Integer page, Integer offset) {

        String token = gitHubApiUtils.decryptToken(user.getGithubToken());

        // 사용자의 GitHub 사용자명 가져오기
        String authorUsername = gitHubApiUtils.getUsernameFromToken(token);

        // 날짜 파싱 및 ISO 형식으로 변환
        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(date);
            log.info("입력 날짜 '{}' 파싱 성공", date);
        } catch (DateTimeException e) {
            log.error("날짜 파싱 오류: {}", e.getMessage());
            throw new IllegalArgumentException(TilMessageCode.GITHUB_INVALID_DATE_FORMAT.getMessage());
        }

        LocalDateTime startDateTime = requestedDate.atStartOfDay();
        LocalDateTime endDateTime = requestedDate.plusDays(1).atStartOfDay();

        String sinceIso = startDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String untilIso = endDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        log.info("조회 기간: {} ~ {}", sinceIso, untilIso);

        //organizationId 관계없이 repositoryId 단독 조회)
        Map<String, Object> repoMeta = gitHubApiUtils.getRepositoryById(repositoryId, token);
        String repoName = (String) repoMeta.get("name");
        String owner = ((Map<String, Object>) repoMeta.get("owner")).get("login").toString();

        String username = gitHubApiUtils.getUsernameFromToken(token);

        return fetchCommitSummary(username, date, repoName, owner, branch, sinceIso, untilIso,
                token, authorUsername, page, offset);
    }


    /**
     * 커밋 요약 정보(SHA, 메시지)만 가져오는 메서드]
     */
    private CommitSummaryResponseDTO.CommitSummaryResponse fetchCommitSummary(String username,
                                                                              String date,
                                                                              String repoName, String owner, String branch,
                                                                              String sinceIso, String untilIso, String token,
                                                                              String authorUsername, Integer page, Integer offset) {

        // GitHub API는 1부터 시작하므로 +1 해서 전달
        int githubApiPage = page + 1;

        // 작성자 필터(author)를 추가하고 페이지네이션을 적용한 URL 구성
        String commitsUrl = GitHubApiConstants.REPOS_BASE_URL + owner + "/" + repoName + "/commits"
                + "?sha=" + branch
                + "&since=" + sinceIso
                + "&until=" + untilIso
                + "&author=" + authorUsername
                + "&page=" + githubApiPage
                + "&per_page=" + offset;

        log.info("GitHub 커밋 요약 API 호출: {}", commitsUrl);

        Map<String, Object>[] commits;
        try {
            commits = webClient.get()
                    .uri(commitsUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().bodyToMono(Map[].class).block();

            log.info("GitHub 커밋 API 응답 수신: {} 개의 커밋", commits != null ? commits.length : 0);
        } catch (WebClientResponseException e) {
            log.error("GitHub API 호출 실패: {} - {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("커밋 조회 오류: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        }

        // 조회된 커밋이 없는 경우 빈 응답 반환
        if (commits == null || commits.length == 0) {
            log.info("날짜 {} 에 해당하는 커밋이 없습니다.", date);
            return CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                    .username(username)
                    .date(date)
                    .repo(repoName)
                    .owner(owner)
                    .commits(Collections.emptyList())
                    .currentPage(page)
                    .pageSize(offset)
                    .currentPageSize(0)
                    .hasNext(false)
                    .build();
        }

        // 커밋 요약 정보 추출
        List<CommitSummaryResponseDTO.CommitSummary> commitSummaries = new ArrayList<>();
        for (Map<String, Object> commit : commits) {
            String sha = commit.get("sha").toString();
            String message = ((Map<String, Object>) commit.get("commit")).get("message").toString();

            // GitHub API에서 날짜 형식 확인
            Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
            Map<String, Object> committer = (Map<String, Object>) commitData.get("committer");
            String commitDateStr = committer.get("date").toString();

            // 커밋 날짜가 입력된 날짜와 일치하는지 확인
            try {
                OffsetDateTime commitDate = OffsetDateTime.parse(commitDateStr,
                        GITHUB_COMMIT_DATE_FORMATTER);
                LocalDate commitLocalDate = commitDate.toLocalDate();
                LocalDate requestedDate = LocalDate.parse(date);

                if (!commitLocalDate.isEqual(requestedDate)) {
                    log.info("커밋 날짜 {}가 요청 날짜 {}와 일치하지 않음, 건너뜀",
                            commitLocalDate, requestedDate);
                    continue;
                }

                log.info("커밋 {}: 날짜 {} 일치 확인됨", sha, commitLocalDate);
            } catch (Exception e) {
                log.warn("커밋 날짜 파싱 오류 (sha={}): {}", sha, e.getMessage());
                // 날짜 파싱 오류가 발생하더라도 계속 진행
            }

            // 작성자 필터링 이중 확인 (URL에 이미 author 파라미터가 포함되었지만 추가 검증)
            Map<String, Object> authorInfo = (Map<String, Object>) commit.get("author");
            if (authorInfo != null && !authorUsername.equals(authorInfo.get("login"))) {
                log.info("본인이 작성한 커밋이 아님: sha={}, author={}", sha, authorInfo.get("login"));
                continue;
            }

            CommitSummaryResponseDTO.CommitSummary commitSummary = CommitSummaryResponseDTO.CommitSummary.builder()
                    .sha(sha)
                    .commitMessage(message)
                    .build();

            commitSummaries.add(commitSummary);
        }

        // 다음 페이지 존재 여부 판단: GitHub API에서 반환한 커밋 수가 요청한 per_page와 같으면 다음 페이지가 있을 가능성
        boolean hasNext = commits.length == offset;

        return CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                .username(username)
                .date(date)
                .repo(repoName)
                .owner(owner)
                .commits(commitSummaries)
                .currentPage(page)
                .pageSize(offset)
                .currentPageSize(commitSummaries.size())
                .hasNext(hasNext)
                .build();
    }
}

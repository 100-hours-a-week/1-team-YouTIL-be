package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Constants.GitHubApiConstants;
import static com.youtil.Api.Github.Constants.GitHubApiConstants.BASE_URL;
import static com.youtil.Api.Github.Constants.GithubCacheConstants.COMMIT_CACHE_KEY;
import static com.youtil.Api.Github.Constants.GithubCacheConstants.COMMIT_CACHE_TTL;
import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Api.Github.Util.GitHubCacheHelper;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Exception.GithubException.GitHubExceptions.GitHubApiException;
import com.youtil.Exception.GithubException.GitHubExceptions.GitHubValidationException;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
public class GithubCommitSummaryService {

    private static final DateTimeFormatter GITHUB_COMMIT_DATE_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

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
            int page, int offset) {

        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        String cacheKey = buildCommitSummaryCacheKey(userId, repositoryId, branch, date, page,
                offset);

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
    private String buildCommitSummaryCacheKey(Long userId, Long repositoryId, String branch,
            String date, int page, int offset) {
        return String.format("%s%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                COMMIT_CACHE_KEY, userId, repositoryId, branch, date, page, offset);
    }

    /**
     * GitHub API 호출 전 날짜 파싱, 레포 정보 조회
     */
    private CommitSummaryResponseDTO.CommitSummaryResponse fetchCommitSummaryFromGithub(
            User user, Long organizationId, Long repositoryId, String branch, String date, int page,
            int offset) {

        String token = gitHubApiUtils.decryptToken(user.getGithubToken());
        String authorUsername = gitHubApiUtils.getUsernameFromToken(token);

        // 날짜 파싱
        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(date);
            log.info("입력 날짜 '{}' 파싱 성공 (KST 기준)", date);
        } catch (DateTimeException e) {
            log.error("날짜 파싱 오류: {}", e.getMessage());
            throw new GitHubValidationException("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요.");
        }

        // KST 기준으로 하루 시작/끝 시간 설정
        LocalDateTime startDateTime = requestedDate.atStartOfDay();
        LocalDateTime endDateTime = requestedDate.plusDays(1).atStartOfDay();

        // KST를 UTC로 변환하여 GitHub API에 전달
        String sinceIso = startDateTime.atZone(KST_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        String untilIso = endDateTime.atZone(KST_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);

        log.info("조회 기간: {} ~ {}", sinceIso, untilIso);

        Map<String, Object> repoMeta = gitHubApiUtils.getRepositoryById(repositoryId, token);
        String repoName = (String) repoMeta.get("name");
        String owner = ((Map<String, Object>) repoMeta.get("owner")).get("login").toString();

        String username = gitHubApiUtils.getUsernameFromToken(token);

        return fetchCommitSummary(username, date, repoName, owner, branch, sinceIso, untilIso,
                token, authorUsername, page, offset);
    }

    /**
     * 커밋 요약 정보(SHA, 메시지)만 가져오는 메서드
     *
     * @param authorUsername 작성자 필터링을 위한 GitHub 사용자명
     */
    private CommitSummaryResponseDTO.CommitSummaryResponse fetchCommitSummary(String username,
            String date,
            String repoName, String owner, String branch,
            String sinceIso, String untilIso, String token,
            String authorUsername, int page, int offset) {

        int githubApiPage = page + 1;
        String encodedBranch = URLEncoder.encode(branch, StandardCharsets.UTF_8);

        URI uri = URI.create(BASE_URL +
                GitHubApiConstants.REPOS_BASE_URL + owner + "/" + repoName + "/commits" +
                "?sha=" + encodedBranch +
                "&since=" + sinceIso +
                "&until=" + untilIso +
                "&author=" + authorUsername +
                "&page=" + githubApiPage +
                "&per_page=" + offset);


        Map<String, Object>[] commits;
        try {
            commits = webClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().bodyToMono(Map[].class).block();

            log.info("GitHub 커밋 API 응답 수신: {} 개의 커밋", commits != null ? commits.length : 0);
        } catch (WebClientResponseException e) {
            log.error("GitHub API 호출 실패: {} - {}", e.getStatusCode(), e.getMessage());
            throw new GitHubApiException("GitHub API 호출에 실패했습니다: " + e.getMessage(),
                    e.getStatusCode().value());
        } catch (Exception e) {
            log.error("커밋 조회 오류: {}", e.getMessage());
            throw new GitHubApiException("커밋 조회 중 오류가 발생했습니다: " + e.getMessage(), 500);
        }

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
        LocalDate requestedDate = LocalDate.parse(date);

        for (Map<String, Object> commit : commits) {
            String sha = commit.get("sha").toString();
            String message = ((Map<String, Object>) commit.get("commit")).get("message").toString();

            // GitHub API에서 날짜 형식 확인
            Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
            Map<String, Object> committer = (Map<String, Object>) commitData.get("committer");
            String commitDateStr = committer.get("date").toString();

            try {
                OffsetDateTime utcCommitDateTime = OffsetDateTime.parse(commitDateStr,
                        GITHUB_COMMIT_DATE_FORMATTER);
                LocalDate kstCommitDate = utcCommitDateTime
                        .atZoneSameInstant(KST_ZONE)
                        .toLocalDate();

                if (!kstCommitDate.isEqual(requestedDate)) {
                    log.info("커밋 날짜 {}(KST)가 요청 날짜 {}와 일치하지 않음, 건너뜀",
                            kstCommitDate, requestedDate);
                    continue;
                }

                log.info("커밋 {}: KST 날짜 {} 일치 확인됨", sha, kstCommitDate);
            } catch (Exception e) {
                log.warn("커밋 날짜 파싱 오류 (sha={}): {}", sha, e.getMessage());
            }

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

package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * 특정 날짜의 커밋 요약 정보(SHA, 메시지)만 조회
     */
    public CommitSummaryResponseDTO.CommitSummaryResponse getCommitSummary(Long userId,
                                                                           Long organizationId,
                                                                           Long repositoryId, String branch, String date,
                                                                           Integer page, Integer offset) {

        // 입력 파라미터 검증
        validateParameters(userId, repositoryId, branch, date);

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String token = decryptToken(user.getGithubToken());
        String authorUsername = getUsernameFromToken(token);
        if ("unknown".equals(authorUsername)) {
            log.warn("GitHub 사용자명을 가져올 수 없습니다. userId={}", userId);
        }

        // KST 날짜를 UTC 범위로 변환
        String[] utcRange = convertKstDateToUtcRange(date);
        String sinceIso = utcRange[0];
        String untilIso = utcRange[1];

        log.info("KST 날짜 '{}' -> UTC 범위: {} ~ {}, 페이지: {}, 사이즈: {}",
                date, sinceIso, untilIso, page, offset);


        // repositoryId로 레포지토리 정보 조회

        Map<String, Object> repoMeta = getRepositoryById(repositoryId, token);
        String repoName = extractRepoName(repoMeta);
        String owner = extractOwner(repoMeta);

        String username = getUsernameFromToken(token);

        return fetchCommitSummary(username, date, repoName, owner, branch, sinceIso, untilIso,
                token, authorUsername, page, offset);
    }

    /**
     * KST 날짜를 UTC 시작/끝 시간으로 변환
     */
    private String[] convertKstDateToUtcRange(String kstDate) {
        try {
            LocalDate date = LocalDate.parse(kstDate);
            LocalDateTime kstStartOfDay = date.atStartOfDay();
            LocalDateTime kstEndOfDay = date.atTime(23, 59, 59);

            String utcStart = kstStartOfDay.atZone(KST_ZONE)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);

            String utcEnd = kstEndOfDay.atZone(KST_ZONE)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_INSTANT);

            return new String[]{utcStart, utcEnd};
        } catch (Exception e) {
            log.error("KST to UTC 범위 변환 실패: kstDate={}", kstDate);
            throw new IllegalArgumentException(TilMessageCode.GITHUB_INVALID_DATE_FORMAT.getMessage());
        }
    }

    /**
     * UTC 시간 문자열을 KST 날짜 문자열로 변환
     */
    private String convertUtcToKstDate(String utcDateString) {
        try {
            Instant instant = Instant.parse(utcDateString);
            return instant.atZone(KST_ZONE).toLocalDate().toString();
        } catch (Exception e) {
            log.warn("UTC to KST 변환 실패: {}", utcDateString);
            return null;
        }
    }

    /**
     * 커밋 요약 정보(SHA, 메시지)만 가져오는 메서드 (KST 시간대 처리)
     */
    private CommitSummaryResponseDTO.CommitSummaryResponse fetchCommitSummary(String username,
                                                                              String date,
                                                                              String repoName, String owner, String branch,
                                                                              String sinceIso, String untilIso, String token,
                                                                              String authorUsername, Integer page, Integer offset) {

        int githubApiPage = page + 1;

        String commitsUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/commits"
                + "?sha=" + branch
                + "&since=" + sinceIso
                + "&until=" + untilIso
                + "&author=" + authorUsername
                + "&page=" + githubApiPage
                + "&per_page=" + offset;

        log.info("GitHub 커밋 요약 API 호출: {}", commitsUrl);

        Map<String, Object>[] commits = fetchCommitsFromGitHub(commitsUrl, token);

        // 조회된 커밋이 없는 경우 빈 응답 반환
        if (commits == null || commits.length == 0) {
            log.info("날짜 {} 에 해당하는 커밋이 없습니다.", date);
            return buildEmptyResponse(username, date, repoName, owner);
        }

        // 커밋 요약 정보 추출
        List<CommitSummaryResponseDTO.CommitSummary> commitSummaries =
                processCommits(commits, date, authorUsername);

        return CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                .username(username)
                .date(date)
                .repo(repoName)
                .owner(owner)
                .commits(commitSummaries)
                .build();
    }

    /**
     * GitHub API에서 커밋 정보 조회
     */
    private Map<String, Object>[] fetchCommitsFromGitHub(String commitsUrl, String token) {
        try {
            Map<String, Object>[] commits = webClient.get()
                    .uri(commitsUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            log.info("GitHub 커밋 API 응답: {} 개의 커밋", commits != null ? commits.length : 0);
          
        } catch (WebClientResponseException e) {
            log.error("GitHub API 호출 실패: {} - {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("커밋 조회 오류: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        }
    }


    /**
     * 빈 응답 생성
     */
    private CommitSummaryResponseDTO.CommitSummaryResponse buildEmptyResponse(
            String username, String date, String repoName, String owner) {
        return CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                .username(username)
                .date(date)
                .repo(repoName)
                .owner(owner)
                .commits(Collections.emptyList())
                .build();
    }

    /**
     * 커밋 배열 처리
     */
    private List<CommitSummaryResponseDTO.CommitSummary> processCommits(
            Map<String, Object>[] commits, String date, String authorUsername) {

        List<CommitSummaryResponseDTO.CommitSummary> commitSummaries = new ArrayList<>();
        LocalDate requestedDate = LocalDate.parse(date);

        for (Map<String, Object> commit : commits) {
            if (commit == null) {
                log.warn("null 커밋이 발견되었습니다. 건너뜁니다.");

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

            // 커밋 날짜를 KST로 변환하여 검증
            Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
            Map<String, Object> committer = (Map<String, Object>) commitData.get("committer");
            String commitDateStr = committer.get("date").toString();

            String kstDateStr = convertUtcToKstDate(commitDateStr);
            if (kstDateStr != null && !kstDateStr.equals(date)) {
                log.debug("커밋 KST 날짜 {}가 요청 날짜 {}와 불일치, 건너뜀", kstDateStr, date);
                continue;
            }

            // 작성자 필터링 이중 확인
            Map<String, Object> authorInfo = (Map<String, Object>) commit.get("author");
            if (authorInfo != null && !authorUsername.equals(authorInfo.get("login"))) {
                log.debug("본인이 작성한 커밋이 아님: sha={}, author={}", sha, authorInfo.get("login"));
                continue;
            }

            Optional<CommitSummaryResponseDTO.CommitSummary> processedCommit =
                    processCommit(commit, requestedDate, authorUsername);

            processedCommit.ifPresent(commitSummaries::add);
        }

        return commitSummaries;

            commitSummaries.add(commitSummary);
            log.debug("커밋 추가: sha={}, KST날짜={}", sha, kstDateStr);
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

    /**
     * GitHub 토큰으로 사용자명 조회
     */
    private String getUsernameFromToken(String token) {
        try {
            Map<String, Object> userInfo = webClient.get()
                    .uri("https://api.github.com/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return Optional.ofNullable(userInfo)
                    .map(info -> info.get("login"))
                    .map(Object::toString)
                    .orElse("unknown");
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 조직 로그인명 조회 (사용되지 않지만 기존 코드 유지)
     */
    private String getOrganizationLogin(Long organizationId, String token) {
        try {
            Map<String, Object>[] orgs = webClient.get()
                    .uri("https://api.github.com/user/orgs")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map[].class)
                    .block();

            if (orgs == null) {
                throw new RuntimeException(TilMessageCode.GITHUB_ORG_NOT_FOUND.getMessage());
            }

            for (Map<String, Object> org : orgs) {
                if (org != null && organizationId.equals(
                        Optional.ofNullable(org.get("id"))
                                .map(Object::toString)
                                .map(Long::valueOf)
                                .orElse(null))) {
                    return Optional.ofNullable(org.get("login"))
                            .map(Object::toString)
                            .orElse(null);
                }
            }

            throw new RuntimeException(TilMessageCode.GITHUB_ORG_NOT_FOUND.getMessage());
        } catch (Exception e) {
            log.error("조직 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_ORG_NOT_FOUND.getMessage());
        }
    }

     /**
     * 토큰 유효성 검증
     */
    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_MISSING.getMessage());
        }
    }

    /**
     * 토큰 복호화
     */
    private String decryptToken(String token) {
        try {
            return tokenEncryptor.decrypt(token);
        } catch (Exception e) {
            log.error("토큰 복호화 실패: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_TOKEN_DECRYPT_ERROR.getMessage());
        }
    }

    /**
     * Repository ID로 정보 조회
     */
    private Map<String, Object> getRepositoryById(Long repositoryId, String token) {
        try {
            Map<String, Object> repoMeta = webClient.get()
                    .uri("https://api.github.com/repositories/" + repositoryId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (repoMeta == null) {
                throw new RuntimeException("Repository metadata is null");
            }

            return repoMeta;
        } catch (WebClientResponseException e) {
            log.error("레포지토리 조회 실패: ID={}, 상태코드={}, 메시지={}",
                    repositoryId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("레포지토리 조회 중 예외 발생: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage() + ": " + e.getMessage());

            log.error("레포지토리 조회 실패: ID={}, 상태코드={}", repositoryId, e.getStatusCode());
            throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage());

        }
    }
}
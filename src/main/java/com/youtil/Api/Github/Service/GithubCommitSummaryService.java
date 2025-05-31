package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
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
    private final WebClient webClient;
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;

    /**
     * 특정 날짜의 커밋 요약 정보(SHA, 메시지)만 조회
     */
    public CommitSummaryResponseDTO.CommitSummaryResponse getCommitSummary(Long userId,
                                                                           Long organizationId,
                                                                           Long repositoryId, String branch, String date) {

        // 입력 파라미터 검증
        validateParameters(userId, repositoryId, branch, date);

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String token = decryptToken(user.getGithubToken());

        // 사용자의 GitHub 사용자명 가져오기
        String authorUsername = getUsernameFromToken(token);
        if ("unknown".equals(authorUsername)) {
            log.warn("GitHub 사용자명을 가져올 수 없습니다. userId={}", userId);
        }

        // 날짜 파싱 및 ISO 형식으로 변환
        LocalDate requestedDate = parseDate(date);

        LocalDateTime startDateTime = requestedDate.atStartOfDay();
        LocalDateTime endDateTime = requestedDate.plusDays(1).atStartOfDay();

        String sinceIso = startDateTime.atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        String untilIso = endDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        log.info("조회 기간: {} ~ {}", sinceIso, untilIso);

        // Repository 정보 조회 (organizationId 관계없이 repositoryId 단독 조회)
        Map<String, Object> repoMeta = getRepositoryById(repositoryId, token);
        String repoName = extractRepoName(repoMeta);
        String owner = extractOwner(repoMeta);

        String username = getUsernameFromToken(token);

        return fetchCommitSummary(username, date, repoName, owner, branch, sinceIso, untilIso,
                token, authorUsername);
    }

    /**
     * 입력 파라미터 검증
     */
    private void validateParameters(Long userId, Long repositoryId, String branch, String date) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (repositoryId == null) {
            throw new IllegalArgumentException(TilMessageCode.TIL_REPOSITORY_ID_REQUIRED.getMessage());
        }
        if (branch == null || branch.trim().isEmpty()) {
            throw new IllegalArgumentException("브랜치명은 필수입니다.");
        }
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("날짜는 필수입니다.");
        }
    }

    /**
     * 날짜 파싱
     */
    private LocalDate parseDate(String date) {
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            log.info("입력 날짜 '{}' 파싱 성공", date);
            return parsedDate;
        } catch (DateTimeException e) {
            log.error("날짜 파싱 오류: {}", e.getMessage());
            throw new IllegalArgumentException(
                    TilMessageCode.GITHUB_INVALID_DATE_FORMAT.getMessage());
        }
    }

    /**
     * Repository 메타데이터에서 이름 추출
     */
    private String extractRepoName(Map<String, Object> repoMeta) {
        return Optional.ofNullable(repoMeta)
                .map(meta -> meta.get("name"))
                .map(Object::toString)
                .orElseThrow(() -> new RuntimeException("Repository name not found"));
    }

    /**
     * Repository 메타데이터에서 owner 추출
     */
    private String extractOwner(Map<String, Object> repoMeta) {
        return Optional.ofNullable(repoMeta)
                .map(meta -> meta.get("owner"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(ownerMap -> ownerMap.get("login"))
                .map(Object::toString)
                .orElseThrow(() -> new RuntimeException("Repository owner not found"));
    }

    /**
     * 커밋 요약 정보(SHA, 메시지)만 가져오는 메서드
     */
    private CommitSummaryResponseDTO.CommitSummaryResponse fetchCommitSummary(String username,
                                                                              String date,
                                                                              String repoName, String owner, String branch,
                                                                              String sinceIso, String untilIso, String token,
                                                                              String authorUsername) {

        // 작성자 필터(author)를 추가한 URL 구성
        String commitsUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/commits"
                + "?sha=" + branch + "&since=" + sinceIso + "&until=" + untilIso + "&author="
                + authorUsername;

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

            log.info("GitHub 커밋 API 응답 수신: {} 개의 커밋", commits != null ? commits.length : 0);
            return commits;
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
                continue;
            }

            Optional<CommitSummaryResponseDTO.CommitSummary> processedCommit =
                    processCommit(commit, requestedDate, authorUsername);

            processedCommit.ifPresent(commitSummaries::add);
        }

        return commitSummaries;
    }

    /**
     * 개별 커밋 처리
     */
    private Optional<CommitSummaryResponseDTO.CommitSummary> processCommit(
            Map<String, Object> commit, LocalDate requestedDate, String authorUsername) {

        // SHA 추출
        String sha = extractSha(commit);
        if (sha == null) {
            log.warn("커밋 SHA가 null입니다. 건너뜁니다.");
            return Optional.empty();
        }

        // 메시지 추출
        String message = extractCommitMessage(commit);

        // 커밋 날짜 검증
        if (!isCommitDateValid(commit, requestedDate, sha)) {
            return Optional.empty();
        }

        // 작성자 검증
        if (!isAuthorValid(commit, authorUsername, sha)) {
            return Optional.empty();
        }

        CommitSummaryResponseDTO.CommitSummary commitSummary =
                CommitSummaryResponseDTO.CommitSummary.builder()
                        .sha(sha)
                        .commitMessage(message)
                        .build();

        return Optional.of(commitSummary);
    }

    /**
     * 커밋에서 SHA 추출
     */
    private String extractSha(Map<String, Object> commit) {
        return Optional.ofNullable(commit.get("sha"))
                .map(Object::toString)
                .orElse(null);
    }

    /**
     * 커밋에서 메시지 추출
     */
    private String extractCommitMessage(Map<String, Object> commit) {
        return Optional.ofNullable(commit.get("commit"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(commitData -> commitData.get("message"))
                .map(Object::toString)
                .orElse("No message");
    }

    /**
     * 커밋 날짜 검증
     */
    private boolean isCommitDateValid(Map<String, Object> commit, LocalDate requestedDate, String sha) {
        String commitDateStr = Optional.ofNullable(commit.get("commit"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(commitData -> commitData.get("committer"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(committer -> committer.get("date"))
                .map(Object::toString)
                .orElse(null);

        if (commitDateStr == null) {
            log.warn("커밋 날짜가 null입니다. sha={}", sha);
            return false;
        }

        try {
            OffsetDateTime commitDate = OffsetDateTime.parse(commitDateStr, GITHUB_COMMIT_DATE_FORMATTER);
            LocalDate commitLocalDate = commitDate.toLocalDate();

            if (!commitLocalDate.isEqual(requestedDate)) {
                log.info("커밋 날짜 {}가 요청 날짜 {}와 일치하지 않음, 건너뜀",
                        commitLocalDate, requestedDate);
                return false;
            }

            log.info("커밋 {}: 날짜 {} 일치 확인됨", sha, commitLocalDate);
            return true;
        } catch (Exception e) {
            log.warn("커밋 날짜 파싱 오류 (sha={}): {}", sha, e.getMessage());
            return false;
        }
    }

    /**
     * 작성자 검증
     */
    private boolean isAuthorValid(Map<String, Object> commit, String authorUsername, String sha) {
        String authorLogin = Optional.ofNullable(commit.get("author"))
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(authorInfo -> authorInfo.get("login"))
                .map(Object::toString)
                .orElse(null);

        if (!authorUsername.equals(authorLogin)) {
            log.info("본인이 작성한 커밋이 아님: sha={}, author={}", sha, authorLogin);
            return false;
        }

        return true;
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
        }
    }
}
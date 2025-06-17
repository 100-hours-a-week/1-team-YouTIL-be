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
                                                                           Long repositoryId, String branch, String date,
                                                                           Integer page, Integer offset) {

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String token = decryptToken(user.getGithubToken());

        // 사용자의 GitHub 사용자명 가져오기
        String authorUsername = getUsernameFromToken(token);

        // 날짜 파싱 및 ISO 형식으로 변환
        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(date);
            log.info("입력 날짜 '{}' 파싱 성공", date);
        } catch (DateTimeException e) {
            log.error("날짜 파싱 오류: {}", e.getMessage());
            throw new IllegalArgumentException(
                    TilMessageCode.GITHUB_INVALID_DATE_FORMAT.getMessage());
        }

        LocalDateTime startDateTime = requestedDate.atStartOfDay();
        LocalDateTime endDateTime = requestedDate.plusDays(1).atStartOfDay();

        String sinceIso = startDateTime.atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        String untilIso = endDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        log.info("조회 기간: {} ~ {}, 페이지: {}, 사이즈: {}", sinceIso, untilIso, page, offset);

        // organizationId 관계없이 repositoryId 단독 조회
        Map<String, Object> repoMeta = getRepositoryById(repositoryId, token);
        String repoName = (String) repoMeta.get("name");
        String owner = ((Map<String, Object>) repoMeta.get("owner")).get("login").toString();

        String username = getUsernameFromToken(token);

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
                                                                              String authorUsername, Integer page, Integer offset) {

        // GitHub API는 1부터 시작하므로 +1 해서 전달
        int githubApiPage = page + 1;

        // 작성자 필터(author)를 추가하고 페이지네이션을 적용한 URL 구성
        String commitsUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/commits"
                + "?sha=" + branch
                + "&since=" + sinceIso
                + "&until=" + untilIso
                + "&author=" + authorUsername
                + "&page=" + githubApiPage
                + "&per_page=" + offset;

        log.info("GitHub 커밋 요약 API 호출 (페이지네이션): {}", commitsUrl);

        Map<String, Object>[] commits;
        try {
            commits = webClient.get()
                    .uri(commitsUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().bodyToMono(Map[].class).block();

            log.info("GitHub 커밋 API 응답 수신: {} 개의 커밋 (GitHub API 페이지: {})",
                    commits != null ? commits.length : 0, githubApiPage);
        } catch (WebClientResponseException e) {
            log.error("GitHub API 호출 실패: {} - {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("커밋 조회 오류: {}", e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        }

        // 조회된 커밋이 없는 경우 빈 응답 반환 (페이지네이션 메타 정보 포함)
        if (commits == null || commits.length == 0) {
            log.info("날짜 {} 에 해당하는 커밋이 없습니다. (페이지: {})", date, page);
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

    private String getUsernameFromToken(String token) {
        Map<String, Object> userInfo = webClient.get()
                .uri("https://api.github.com/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().bodyToMono(Map.class).block();

        return userInfo != null ? userInfo.get("login").toString() : "unknown";
    }

    private String getOrganizationLogin(Long organizationId, String token) {
        Map<String, Object>[] orgs = webClient.get()
                .uri("https://api.github.com/user/orgs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        for (Map<String, Object> org : orgs) {
            if (Long.valueOf(org.get("id").toString()).equals(organizationId)) {
                return org.get("login").toString();
            }
        }

        throw new RuntimeException(TilMessageCode.GITHUB_ORG_NOT_FOUND.getMessage());
    }

    private String getRepositoryNameFromOrg(String owner, Long repositoryId, String token) {
        Map<String, Object>[] repos = webClient.get()
                .uri("https://api.github.com/orgs/" + owner + "/repos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        for (Map<String, Object> repo : repos) {
            if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                return repo.get("name").toString();
            }
        }

        throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage());
    }

    private Map.Entry<String, String> getPersonalRepoInfo(Long repositoryId, String token) {
        Map<String, Object>[] repos = webClient.get()
                .uri("https://api.github.com/user/repos?affiliation=owner,collaborator")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Map[].class)
                .block();

        for (Map<String, Object> repo : repos) {
            if (Long.valueOf(repo.get("id").toString()).equals(repositoryId)) {
                String repoName = repo.get("name").toString();
                String owner = ((Map<String, Object>) repo.get("owner")).get("login").toString();
                return Map.entry(owner, repoName);
            }
        }

        throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage());
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
            log.error("레포지토리 조회 실패: ID={}, 상태코드={}, 메시지={}",
                    repositoryId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException(TilMessageCode.GITHUB_REPO_NOT_FOUND.getMessage() + ": " + e.getMessage());
        }
    }
}

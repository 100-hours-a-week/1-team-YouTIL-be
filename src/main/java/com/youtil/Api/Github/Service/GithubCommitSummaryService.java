package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Model.User;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubCommitSummaryService {

    private final WebClient webClient;
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;

    private static final DateTimeFormatter GITHUB_COMMIT_DATE_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * 특정 날짜의 커밋 요약 정보(SHA, 메시지)만 조회
     */
    public CommitSummaryResponseDTO.CommitSummaryResponse getCommitSummary(Long userId, Long organizationId,
                                                                           Long repositoryId, String branch, String date) {

        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String token = decryptToken(user.getGithubToken());

        // 날짜 파싱 및 ISO 형식으로 변환
        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식이어야 합니다.");
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("유효하지 않은 날짜입니다: " + e.getMessage());
        }

        // 시작 날짜와 종료 날짜 계산
        LocalDateTime startDateTime = requestedDate.atStartOfDay();
        LocalDateTime endDateTime = requestedDate.plusDays(1).atStartOfDay();

        // ISO 형식으로 변환
        String sinceIso = startDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String untilIso = endDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        String owner;
        String repoName;

        if (organizationId != null) {
            owner = getOrganizationLogin(organizationId, token);
            repoName = getRepositoryNameFromOrg(owner, repositoryId, token);
        } else {
            Map.Entry<String, String> result = getPersonalRepoInfo(repositoryId, token);
            owner = result.getKey();
            repoName = result.getValue();
        }

        String username = getUsernameFromToken(token);

        return fetchCommitSummary(username, date, repoName, owner, branch, sinceIso, untilIso, token);
    }

    /**
     * 커밋 요약 정보(SHA, 메시지)만 가져오는 메서드
     */
    private CommitSummaryResponseDTO.CommitSummaryResponse fetchCommitSummary(String username, String date,
                                                                              String repoName, String owner, String branch,
                                                                              String sinceIso, String untilIso, String token) {

        String commitsUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/commits"
                + "?sha=" + branch + "&since=" + sinceIso + "&until=" + untilIso;

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
            throw new RuntimeException("GitHub API 호출 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("커밋 조회 오류: {}", e.getMessage());
            throw new RuntimeException("커밋 조회 중 오류가 발생했습니다: " + e.getMessage());
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
                OffsetDateTime commitDate = OffsetDateTime.parse(commitDateStr, GITHUB_COMMIT_DATE_FORMATTER);
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

            CommitSummaryResponseDTO.CommitSummary commitSummary = CommitSummaryResponseDTO.CommitSummary.builder()
                    .sha(sha)
                    .commitMessage(message)
                    .build();

            commitSummaries.add(commitSummary);
        }

        return CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                .username(username)
                .date(date)
                .repo(repoName)
                .owner(owner)
                .commits(commitSummaries)
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

        throw new RuntimeException("해당 조직을 찾을 수 없습니다.");
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

        throw new RuntimeException("조직 내에서 레포지토리를 찾을 수 없습니다.");
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

        throw new RuntimeException("개인 레포지토리를 찾을 수 없습니다.");
    }

    private void validateToken(User user) {
        if (user.getGithubToken() == null || user.getGithubToken().isEmpty()) {
            throw new RuntimeException("GitHub 토큰이 없습니다.");
        }
    }

    private String decryptToken(String token) {
        try {
            return tokenEncryptor.decrypt(token);
        } catch (Exception e) {
            throw new RuntimeException("GitHub 토큰 복호화 실패");
        }
    }
}
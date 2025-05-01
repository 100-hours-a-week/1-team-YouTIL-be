package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitResponseDTO;
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
public class GithubCommitService {

    private final WebClient webClient;
    private final TokenEncryptor tokenEncryptor;
    private final EntityValidator entityValidator;

    private static final DateTimeFormatter GITHUB_COMMIT_DATE_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public CommitResponseDTO getCommits(Long userId, Long organizationId, Long repositoryId, String branch, String date) {
        User user = entityValidator.getValidUserOrThrow(userId);
        validateToken(user);

        String token = decryptToken(user.getGithubToken());

        // 날짜 파싱 및 ISO 형식으로 변환
        LocalDate requestedDate;
        try {
            requestedDate = LocalDate.parse(date);
            log.info("입력 날짜 '{}' 파싱 성공", date);
        } catch (DateTimeParseException e) {
            log.error("날짜 파싱 오류: {}", e.getMessage());
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식이어야 합니다.");
        } catch (DateTimeException e) {
            log.error("유효하지 않은 날짜: {}", e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 날짜입니다: " + e.getMessage());
        }

        // 시작 날짜와 종료 날짜 계산
        LocalDateTime startDateTime = requestedDate.atStartOfDay();
        LocalDateTime endDateTime = requestedDate.plusDays(1).atStartOfDay();

        // ISO 형식으로 변환
        String sinceIso = startDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String untilIso = endDateTime.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        log.info("조회 기간: {} ~ {}", sinceIso, untilIso);

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

        return fetchCommits(username, date, repoName, owner, branch, sinceIso, untilIso, token);
    }

    private String getUsernameFromToken(String token) {
        Map<String, Object> userInfo = webClient.get()
                .uri("https://api.github.com/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve().bodyToMono(Map.class).block();

        return userInfo != null ? userInfo.get("login").toString() : "unknown";
    }

    private CommitResponseDTO fetchCommits(String username, String date, String repoName,
                                           String owner, String branch, String sinceIso,
                                           String untilIso, String token) {
        String commitsUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/commits"
                + "?sha=" + branch + "&since=" + sinceIso + "&until=" + untilIso;

        log.info("GitHub 커밋 API 호출: {}", commitsUrl);

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
            return CommitResponseDTO.builder()
                    .username(username)
                    .date(date)
                    .repo(repoName)
                    .files(Collections.emptyList())
                    .build();
        }

        // 파일별로 커밋 정보를 수집하기 위한 맵
        Map<String, List<CommitResponseDTO.CommitInfo>> fileCommitsMap = new HashMap<>();
        Map<String, String> fileLatestContentMap = new HashMap<>();

        // 모든 커밋을 순회하며 파일별 정보 수집
        for (Map<String, Object> commit : commits) {
            String sha = commit.get("sha").toString();

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

            // 상세 정보 조회
            Map<String, Object> detail;
            try {
                detail = webClient.get()
                        .uri("https://api.github.com/repos/" + owner + "/" + repoName + "/commits/" + sha)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().bodyToMono(Map.class).block();
            } catch (Exception e) {
                log.error("커밋 상세 정보 조회 실패 (sha={}): {}", sha, e.getMessage());
                continue; // 이 커밋은 건너뛰고 다음 커밋 처리
            }

            if (detail == null || !detail.containsKey("commit") || !detail.containsKey("files")) {
                log.warn("커밋 상세 정보가 없거나 필수 필드가 누락됨 (sha={})", sha);
                continue;
            }

            String message = ((Map<String, Object>) detail.get("commit")).get("message").toString();
            List<Map<String, Object>> files = (List<Map<String, Object>>) detail.get("files");
            log.info("커밋 {} 에서 {} 개의 파일 변경 발견", sha, files.size());

            // 각 파일별로 커밋 정보 처리
            for (Map<String, Object> file : files) {
                if (!file.containsKey("filename")) {
                    log.warn("파일 정보에 filename 필드 누락");
                    continue;
                }

                String filepath = file.get("filename").toString();
                String patch = file.getOrDefault("patch", "").toString();

                // 커밋 정보 저장
                CommitResponseDTO.CommitInfo commitInfo = CommitResponseDTO.CommitInfo.builder()
                        .commitMessage(message)
                        .patch(patch)
                        .build();

                fileCommitsMap.computeIfAbsent(filepath, k -> new ArrayList<>()).add(commitInfo);
            }
        }

        // 수집된 파일이 없는 경우
        if (fileCommitsMap.isEmpty()) {
            log.warn("처리 가능한 파일 변경 내역이 없습니다.");
            return CommitResponseDTO.builder()
                    .username(username)
                    .date(date)
                    .repo(repoName)
                    .files(Collections.emptyList())
                    .build();
        }

        // 파일 내용 별도 조회 (최신 브랜치 기준)
        for (String filepath : fileCommitsMap.keySet()) {
            try {
                log.info("파일 {} 내용 조회 시작", filepath);
                String contentUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                        owner, repoName, filepath, branch);

                // URL 인코딩 처리 (공백이나 특수문자가 있는 경우)
                contentUrl = contentUrl.replace(" ", "%20");

                Map<String, Object> contentResponse = webClient.get()
                        .uri(contentUrl)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve().bodyToMono(Map.class).block();

                if (contentResponse != null && contentResponse.containsKey("content")) {
                    String encodedContent = contentResponse.get("content").toString();
                    log.debug("파일 {} Base64 인코딩 내용 수신 (길이: {})", filepath, encodedContent.length());

                    // Base64 문자열에서 불필요한 줄바꿈 제거
                    encodedContent = encodedContent.replaceAll("\\n", "");

                    try {
                        // Base64 디코딩
                        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
                        String decodedContent = new String(decodedBytes);
                        fileLatestContentMap.put(filepath, decodedContent);

                        log.info("파일 {} 내용 로드 성공, 길이: {}", filepath, decodedContent.length());
                    } catch (IllegalArgumentException e) {
                        log.error("Base64 디코딩 실패 ({}): {}", filepath, e.getMessage());
                        fileLatestContentMap.put(filepath, ""); // 디코딩 실패 시 빈 문자열
                    }
                } else {
                    log.warn("파일 {} 내용 필드 없음", filepath);
                    fileLatestContentMap.put(filepath, ""); // 내용 없을 경우 빈 문자열
                }
            } catch (Exception e) {
                log.error("파일 {} 내용 조회 실패: {}", filepath, e.getMessage());
                fileLatestContentMap.put(filepath, ""); // 오류 시 빈 문자열로 설정
            }
        }

        // 파일별로 정보 구성
        List<CommitResponseDTO.FileInfo> fileInfos = new ArrayList<>();
        for (Map.Entry<String, List<CommitResponseDTO.CommitInfo>> entry : fileCommitsMap.entrySet()) {
            String filepath = entry.getKey();
            List<CommitResponseDTO.CommitInfo> commitInfos = entry.getValue();

            // 커밋은 최신순으로 정렬 (제공된 JSON 예시에서는 최신 커밋이 먼저 오는 구조)
            Collections.reverse(commitInfos);

            // 디버깅 로그 추가
            log.info("파일: {}, 최신 코드 길이: {}, 커밋 개수: {}",
                    filepath, fileLatestContentMap.getOrDefault(filepath, "").length(),
                    commitInfos.size());

            CommitResponseDTO.FileInfo fileInfo = CommitResponseDTO.FileInfo.builder()
                    .filepath(filepath)
                    .latestCode(fileLatestContentMap.getOrDefault(filepath, ""))
                    .patches(commitInfos)
                    .build();

            fileInfos.add(fileInfo);
        }

        log.info("응답 생성 완료: {} 개의 파일 정보", fileInfos.size());
        return CommitResponseDTO.builder()
                .username(username)
                .date(date)
                .repo(repoName)
                .files(fileInfos)
                .build();
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
                .uri("https://api.github.com/user/repos?affiliation=owner")
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
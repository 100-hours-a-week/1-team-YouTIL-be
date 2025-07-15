package com.youtil.Api.Tils.Service;

import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Api.Github.Util.GitHubRepositoryConfigUtil;
import com.youtil.Api.Tils.Dto.TilUploadRequestDTO;
import com.youtil.Api.Tils.Dto.TilUploadResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.TilRepository;
import com.youtil.Util.EntityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDate;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilUploadService {

    private final TilRepository tilRepository;
    private final EntityValidator entityValidator;
    private final GitHubApiUtils gitHubApiUtils;
    private final WebClient webClient;

    /**
     * 기본 설정된 레포지토리에 TIL을 업로드합니다
     */
    @Transactional
    public TilUploadResponseDTO.UploadToGitHubResponse uploadTilToGitHub(
            TilUploadRequestDTO.UploadRequest request, Long userId) {

        log.info("TIL GitHub 업로드 시작 (기본 설정 사용) - TIL ID: {}", request.getTilId());

        // 1. 사용자 및 TIL 유효성 검사
        User user = entityValidator.getValidUserOrThrow(userId);
        gitHubApiUtils.validateToken(user);

        Til til = tilRepository.findById(request.getTilId())
                .orElseThrow(() -> new RuntimeException(TilMessageCode.TIL_NOT_FOUND.getMessage()));

        // TIL 소유자 확인
        if (!til.getUser().getId().equals(userId)) {
            throw new RuntimeException(TilMessageCode.TIL_ACCESS_DENIED.getMessage());
        }

        // 삭제된 TIL 확인
        if (til.getStatus() == Status.deactive) {
            throw new RuntimeException(TilMessageCode.TIL_ALREADY_DELETED.getMessage());
        }

        // 2. 기본 레포지토리 설정 확인
        if (user.getUploadRepository() == null || user.getUploadRepository().trim().isEmpty()) {
            throw new RuntimeException("기본 업로드 레포지토리가 설정되지 않았습니다. 먼저 레포지토리를 설정해주세요.");
        }

        // 3. 설정 파싱
        GitHubRepositoryConfigUtil.GitHubRepoConfig config;
        try {
            config = GitHubRepositoryConfigUtil.parseConfig(user.getUploadRepository());
            log.info("기본 레포지토리 설정 파싱 완료 - 조직여부: {}, 레포ID: {}, 브랜치: {}",
                    config.isOrganization(), config.getRepositoryId(), config.getBranch());
        } catch (Exception e) {
            log.error("기본 레포지토리 설정 파싱 실패: {}", e.getMessage());
            throw new RuntimeException("기본 레포지토리 설정이 손상되었습니다. 레포지토리를 다시 설정해주세요.");
        }

        String token = gitHubApiUtils.decryptToken(user.getGithubToken());

        // 4. 레포지토리 정보 조회
        String owner;
        String repoName;

        try {
            Map<String, Object> repoInfo = gitHubApiUtils.getRepositoryById(config.getRepositoryId(), token);

            if (repoInfo == null || !repoInfo.containsKey("name") || !repoInfo.containsKey("owner")) {
                throw new RuntimeException("설정된 레포지토리를 찾을 수 없습니다: " + config.getRepositoryId());
            }

            repoName = repoInfo.get("name").toString();
            owner = ((Map<String, Object>) repoInfo.get("owner")).get("login").toString();

            log.info("레포지토리 정보 조회 완료 - 소유자: {}, 레포: {}", owner, repoName);

        } catch (Exception e) {
            log.error("레포지토리 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("설정된 레포지토리에 접근할 수 없습니다: " + e.getMessage());
        }

        // 5. 마크다운 콘텐츠 생성
        String markdownContent = generateMarkdownContent(til);

        // 6. 파일 경로 자동 생성 (tils/YYYY-MM-DD.md)
        String filePath = generateAutoFilePath(til);

        // 7. 커밋 메시지 자동 생성 (TIL 제목 사용)
        String commitMessage = generateAutoCommitMessage(til);

        try {
            // 8. GitHub에 파일 업로드
            Map<String, Object> uploadResponse = uploadFileToGitHub(
                    owner, repoName, filePath, markdownContent, commitMessage,
                    config.getBranch(), token);

            // 9. TIL 업로드 상태 업데이트
            til.setIsUploaded(true);
            tilRepository.save(til);

            // 10. 응답 생성
            String fileUrl = String.format("https://github.com/%s/%s/blob/%s/%s",
                    owner, repoName, config.getBranch(), filePath);

            String commitSha = extractCommitSha(uploadResponse);

            log.info("TIL GitHub 업로드 완료 - 파일 URL: {}", fileUrl);

            return TilUploadResponseDTO.UploadToGitHubResponse.builder()
                    .success(true)
                    .fileUrl(fileUrl)
                    .commitSha(commitSha)
                    .uploadedFilePath(filePath)
                    .message("TIL이 성공적으로 업로드되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("TIL GitHub 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("GitHub 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 자동 파일 경로 생성: tils/YYYY-MM-DD.md (업로드 날짜 기준)
     */
    private String generateAutoFilePath(Til til) {
        String uploadDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // TIL 제목을 파일명에 안전하게 사용하기 위해 정리
        String safeTitle = sanitizeFileName(til.getTitle());

        // 파일 경로: tils/제목-TILID-날짜.md
        String filePath = String.format("tils/%s_%d_%s.md", safeTitle, til.getId(), uploadDate);

        log.info("자동 생성된 파일 경로 (제목_TILID_날짜): {}", filePath);
        return filePath;
    }

    /**
     * 파일명에 사용할 수 없는 문자들을 안전하게 변환
     */
    private String sanitizeFileName(String title) {
        if (title == null || title.trim().isEmpty()) {
            return "untitled";
        }

        String sanitized = title.trim()
                // 파일명에 사용할 수 없는 특수문자들 제거/변환
                .replaceAll("[<>:\"/\\\\|?*]", "")  // Windows/Linux 금지 문자
                .replaceAll("\\s+", "-")            // 공백을 하이픈으로
                .replaceAll("-+", "-")              // 연속 하이픈을 하나로
                .replaceAll("^-|-$", "");           // 앞뒤 하이픈 제거

        // 빈 문자열이면 기본값 사용
        if (sanitized.isEmpty()) {
            sanitized = "untitled";
        }

        // 파일명 길이 제한 (최대 50자)
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
            // 잘린 부분이 하이픈으로 끝나면 제거
            sanitized = sanitized.replaceAll("-+$", "");
        }

        return sanitized;
    }

    /**
     * 자동 커밋 메시지 생성: TIL 제목 사용
     */
    private String generateAutoCommitMessage(Til til) {
        String commitMessage = til.getTitle();
        log.info("자동 생성된 커밋 메시지: {}", commitMessage);
        return commitMessage;
    }

    /**
     * TIL 내용만 마크다운으로 변환합니다 (메타데이터, 푸터 제외).
     */
    private String generateMarkdownContent(Til til) {
        // TIL 내용만 그대로 반환 (메타데이터, 푸터 없음)
        if (til.getContent() != null && !til.getContent().isEmpty()) {
            // 마크다운 내용을 정규화하여 GitHub에서 제대로 렌더링되도록 함
            String normalizedContent = normalizeMarkdownContent(til.getContent());

            return normalizedContent;
        } else {
            return "*내용이 없습니다.*";
        }
    }

    /**
     * GitHub에 파일을 업로드합니다.
     */
    private Map<String, Object> uploadFileToGitHub(String owner, String repo, String path,
                                                   String content, String message, String branch, String token) {

        // UTF-8로 명시적 인코딩 후 Base64 변환 (마크다운 렌더링 개선)
        String encodedContent;
        try {
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            encodedContent = Base64.getEncoder().encodeToString(contentBytes);
        } catch (Exception e) {
            throw new RuntimeException("파일 내용 인코딩에 실패했습니다: " + e.getMessage());
        }

        // 요청 본문 생성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", message);
        requestBody.put("content", encodedContent);
        requestBody.put("branch", branch);

        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", owner, repo, path);

        try {
            // 기존 파일 존재 여부 확인 (SHA 값 필요)
            try {
                Map<String, Object> existingFile = gitHubApiUtils.callGitHubApi(
                        webClient.get()
                                .uri(url + "?ref=" + branch)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .retrieve()
                                .bodyToMono(Map.class),
                        "기존 파일 확인"
                );

                if (existingFile != null && existingFile.containsKey("sha")) {
                    requestBody.put("sha", existingFile.get("sha"));
                    log.info("기존 파일 발견, SHA 값 포함하여 업데이트: {}", existingFile.get("sha"));
                }
            } catch (Exception e) {
                log.info("기존 파일 없음, 새 파일로 생성");
            }

            // 파일 업로드 (UTF-8 Content-Type 명시)
            Map<String, Object> response = gitHubApiUtils.callGitHubApi(
                    webClient.put()
                            .uri(url)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .header(HttpHeaders.CONTENT_TYPE, "application/json; charset=UTF-8")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(Map.class),
                    "파일 업로드"
            );

            log.info("GitHub 마크다운 파일 업로드 성공");
            return response;

        } catch (Exception e) {
            log.error("GitHub 파일 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("GitHub 파일 업로드에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 업로드 응답에서 커밋 SHA를 추출합니다.
     */
    private String extractCommitSha(Map<String, Object> response) {
        try {
            if (response.containsKey("commit")) {
                Map<String, Object> commit = (Map<String, Object>) response.get("commit");
                return commit.get("sha").toString();
            }
        } catch (Exception e) {
            log.warn("커밋 SHA 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 마크다운 내용을 정규화하여 GitHub에서 제대로 렌더링되도록 합니다.
     */
    private String normalizeMarkdownContent(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 1. 줄바꿈 통일 (\r\n -> \n)
        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");

        // 2. 비표준 공백 문자를 일반 공백으로 변경
        normalized = normalized.replace('\u00A0', ' '); // non-breaking space
        normalized = normalized.replace('\u2007', ' '); // figure space
        normalized = normalized.replace('\u202F', ' '); // narrow no-break space

        // 3. 탭을 4개 공백으로 변경
        normalized = normalized.replace("\t", "    ");

        // 4. 마크다운 헤딩 앞뒤 공백 정리 (# 뒤에 정확히 하나의 공백)
        normalized = normalized.replaceAll("(?m)^(#{1,6})\\s*", "$1 ");

        // 5. 각 줄의 앞뒤 공백 제거 (전체 trim이 아닌 라인별 trim)
        String[] lines = normalized.split("\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            result.append(line);

            // 마지막 줄이 아니면 줄바꿈 추가
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        // 6. 연속된 빈 줄을 최대 2개로 제한
        String finalContent = result.toString().replaceAll("\n{3,}", "\n\n");

        // 7. 앞뒤 공백 제거
        return finalContent.trim();
    }
}

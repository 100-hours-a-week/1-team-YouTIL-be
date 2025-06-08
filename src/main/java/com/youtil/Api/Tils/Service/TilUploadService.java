package com.youtil.Api.Tils.Service;

import com.youtil.Api.Github.Util.GitHubApiUtils;
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
     * TIL을 GitHub 레포지토리에 마크다운 파일로 업로드합니다.
     */
    @Transactional
    public TilUploadResponseDTO.UploadToGitHubResponse uploadTilToGitHub(
            TilUploadRequestDTO.UploadToGitHubRequest request, Long userId) {

        log.info("TIL GitHub 업로드 시작 - TIL ID: {}, 레포지토리 ID: {}, 브랜치: {}",
                request.getTilId(), request.getRepositoryId(), request.getBranch());

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

        String token = gitHubApiUtils.decryptToken(user.getGithubToken());

        // 2. 레포지토리 정보 조회 (조직/개인 구분 없이 통합 처리)
        String owner;
        String repoName;

        try {
            // 레포지토리 ID로 직접 조회 (조직/개인 관계없이)
            Map<String, Object> repoInfo = gitHubApiUtils.getRepositoryById(request.getRepositoryId(), token);

            if (repoInfo == null || !repoInfo.containsKey("name") || !repoInfo.containsKey("owner")) {
                throw new RuntimeException("해당 ID의 레포지토리를 찾을 수 없습니다: " + request.getRepositoryId());
            }

            repoName = repoInfo.get("name").toString();
            owner = ((Map<String, Object>) repoInfo.get("owner")).get("login").toString();

            log.info("레포지토리 정보 조회 완료 - 소유자: {}, 레포: {}", owner, repoName);

        } catch (Exception e) {
            log.error("레포지토리 정보 조회 실패: {}", e.getMessage());
            throw new RuntimeException("레포지토리 정보 조회에 실패했습니다: " + e.getMessage());
        }

        // 3. 마크다운 콘텐츠 생성
        String markdownContent = generateMarkdownContent(til);

        // 4. 파일 경로 생성
        String filePath = generateFilePath(request.getFilePath(), til);

        // 5. 커밋 메시지 생성
        String commitMessage = generateCommitMessage(request.getCommitMessage(), til);

        try {
            // 6. GitHub에 파일 업로드
            Map<String, Object> uploadResponse = uploadFileToGitHub(
                    owner, repoName, filePath, markdownContent, commitMessage,
                    request.getBranch(), token);

            // 7. TIL 업로드 상태 업데이트
            til.setIsUploaded(true);
            tilRepository.save(til);

            // 8. 응답 생성
            String fileUrl = String.format("https://github.com/%s/%s/blob/%s/%s",
                    owner, repoName, request.getBranch(), filePath);

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
     * TIL 내용만 마크다운으로 변환합니다 (메타데이터, 푸터 제외).
     */
    private String generateMarkdownContent(Til til) {
        // TIL 내용만 그대로 반환 (메타데이터, 푸터 없음)
        if (til.getContent() != null && !til.getContent().isEmpty()) {
            // 마크다운 내용을 정규화하여 GitHub에서 제대로 렌더링되도록 함
            return normalizeMarkdownContent(til.getContent());
        } else {
            return "*내용이 없습니다.*";
        }
    }

    /**
     * 파일 경로를 생성합니다.
     */
    private String generateFilePath(String customPath, Til til) {
        String filePath;

        if (customPath != null && !customPath.trim().isEmpty()) {
            filePath = customPath.trim();
        } else {
            // 기본 경로: til/YYYY-MM-DD-title.md
            String date = til.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String filename = sanitizeFilename(til.getTitle());
            filePath = String.format("til/%s-%s.md", date, filename);
        }

        // .md 확장자 강제 적용
        if (!filePath.toLowerCase().endsWith(".md")) {
            filePath = filePath + ".md";
            log.info("파일 확장자 .md 추가: {}", filePath);
        }

        log.info("최종 파일 경로: {}", filePath);
        return filePath;
    }

    /**
     * 파일명에서 특수문자를 제거합니다.
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9가-힣\\s-]", "")
                .replaceAll("\\s+", "-")
                .toLowerCase();
    }

    /**
     * 커밋 메시지를 생성합니다.
     */
    private String generateCommitMessage(String customMessage, Til til) {
        if (customMessage != null && !customMessage.trim().isEmpty()) {
            return customMessage.trim();
        }

        return String.format("docs: %s TIL 추가", til.getTitle());
    }

    /**
     * GitHub에 파일을 업로드합니다.
     */
    private Map<String, Object> uploadFileToGitHub(String owner, String repo, String path,
                                                   String content, String message, String branch, String token) {

        log.info("GitHub API 호출: 파일 업로드 - {}/{}/{}", owner, repo, path);

        // UTF-8로 명시적 인코딩 후 Base64 변환 (마크다운 렌더링 개선)
        String encodedContent;
        try {
            encodedContent = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("콘텐츠 UTF-8 인코딩 실패: {}", e.getMessage());
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

        // 2. 비표준 공백 문자를 일반 공백으로 변경 (GitHub 마크다운 렌더링 문제 해결)
        normalized = normalized.replace('\u00A0', ' '); // non-breaking space
        normalized = normalized.replace('\u2007', ' '); // figure space
        normalized = normalized.replace('\u202F', ' '); // narrow no-break space

        // 3. 탭을 4개 공백으로 변경 (GitHub 표준)
        normalized = normalized.replace("\t", "    ");

        // 4. 연속된 빈 줄을 최대 2개로 제한 (마크다운 가독성)
        normalized = normalized.replaceAll("\n{3,}", "\n\n");

        // 5. 앞뒤 공백 제거
        normalized = normalized.trim();

        // 6. 마크다운 헤딩 앞뒤 공백 정리 (GitHub 렌더링 개선)
        normalized = normalized.replaceAll("(?m)^(#{1,6})\\s+", "$1 ");

        return normalized;
    }
}

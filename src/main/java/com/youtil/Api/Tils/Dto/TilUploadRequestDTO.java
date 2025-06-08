package com.youtil.Api.Tils.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TilUploadRequestDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "TIL GitHub 업로드 요청")
    public static class UploadToGitHubRequest {
        @Schema(description = "업로드할 TIL ID", example = "123", required = true)
        private Long tilId;

        @Schema(description = "조직 ID (선택사항)", example = "12345678")
        private Long organizationId;

        @Schema(description = "레포지토리 ID", example = "98765432", required = true)
        private Long repositoryId;

        @Schema(description = "브랜치명", example = "main", required = true)
        private String branch;

        @Schema(description = "파일 경로 (선택사항, 기본값: til/YYYY-MM-DD-title.md)", example = "docs/til/backend-optimization.md")
        private String filePath;

        @Schema(description = "커밋 메시지 (선택사항)", example = "docs: Redis 초기 세팅 구현 TIL 추가")
        private String commitMessage;
    }
}

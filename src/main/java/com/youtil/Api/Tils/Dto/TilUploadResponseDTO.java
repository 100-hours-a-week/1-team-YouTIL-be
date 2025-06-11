package com.youtil.Api.Tils.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TilUploadResponseDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "TIL GitHub 업로드 응답")
    public static class UploadToGitHubResponse {
        @Schema(description = "업로드 성공 여부", example = "true")
        private Boolean success;

        @Schema(description = "GitHub 파일 URL", example = "https://github.com/username/repo/blob/main/til/2024-12-06-redis-setup.md")
        private String fileUrl;

        @Schema(description = "커밋 SHA", example = "abc123def456ghi789jkl012mno345pqr678stu901")
        private String commitSha;

        @Schema(description = "업로드된 파일 경로", example = "til/2024-12-06-redis-setup.md")
        private String uploadedFilePath;

        @Schema(description = "메시지", example = "TIL이 성공적으로 업로드되었습니다.")
        private String message;
    }
}

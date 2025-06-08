package com.youtil.Api.Github.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class GitHubRepositorySettingDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "GitHub 레포지토리 설정 요청")
    public static class SetRepositoryRequest {
        @Schema(description = "조직 ID (선택사항)", example = "167328634")
        private Long organizationId;

        @Schema(description = "레포지토리 ID", example = "927579728", required = true)
        private Long repositoryId;

        @Schema(description = "브랜치명", example = "main", required = true)
        private String branch;

        // repository 필드 제거됨 - GitHub API에서 자동으로 조회
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "GitHub 레포지토리 설정 응답")
    public static class RepositorySettingResponse {
        @Schema(description = "조직 ID", example = "167328634")
        private Long organizationId;

        @Schema(description = "레포지토리 ID", example = "927579728")
        private Long repositoryId;

        @Schema(description = "레포지토리명", example = "dia-til")
        private String repository;

        @Schema(description = "브랜치명", example = "main")
        private String branch;

        @Schema(description = "소유자명", example = "username-or-orgname")
        private String owner;

        @Schema(description = "설정 여부", example = "true")
        private Boolean isConfigured;

        @Schema(description = "업데이트 일시", example = "2025-06-08T19:30:00Z")
        private String updatedAt;
    }
}

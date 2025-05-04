package com.youtil.Api.Github.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CommitDetailRequestDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선택된 커밋의 상세 정보 요청")
    public static class CommitDetailRequest {
        @Schema(description = "조직 ID (선택사항)", example = "12345678")
        private Long organizationId;

        @Schema(description = "레포지토리 ID", example = "98765432", required = true)
        private Long repositoryId;

        @Schema(description = "브랜치명", example = "master", required = true)
        private String branch;

        @Schema(description = "조회할 커밋 정보 목록", required = true)
        private List<CommitSummary> commits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커밋 요약 정보")
    public static class CommitSummary {
        @Schema(description = "커밋 SHA", example = "321e1e83793d0b8b933befa9ad08b1ac2d150583")
        private String sha;

        @Schema(description = "커밋 메시지", example = "feat: 소셜 로그인 추가")
        private String message;
    }
}
package com.youtil.Api.Tils.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class TilRequestDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTilRequest {
        private String title;
        private String content;
        private String category;
        private List<String> tag;
        private Boolean isDisplay;
        private String commitRepository;
        private Boolean isUploaded;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 생성 TIL 저장 요청")
    public static class CreateAiTilRequest {
        @Schema(description = "GitHub 레포지토리명", example = "Translator_Practice")
        private String repo;

        @Schema(description = "TIL 제목", example = "Redis 초기 세팅 구현")
        private String title;

        @Schema(description = "TIL 카테고리", example = "FULLSTACK")
        private String category;

        @Schema(description = "AI가 생성한 TIL 내용")
        private String content;

        @Schema(description = "AI가 추천한 태그 목록")
        private List<String> tags;

        @JsonProperty("is_shared")
        @Schema(description = "커뮤니티 업로드 여부 (TRUE: 업로드, FALSE: 미업로드)", example = "true")
        private Boolean isShared;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "AI 기반 TIL 생성 통합 요청")
    public static class CreateWithAiRequest {
        @Schema(description = "GitHub 조직 ID")
        private Long organizationId;

        @Schema(description = "GitHub 레포지토리 ID", example = "841051187")
        private Long repositoryId;

        @Schema(description = "브랜치명", example = "master")
        private String branch;

        @Schema(description = "선택한 커밋 목록")
        private List<CommitSummary> commits;

        @Schema(description = "TIL 제목", example = "쿠폰 기능 개선 작업")
        private String title;

        @Schema(description = "TIL 카테고리", example = "BACKEND")
        private String category;

        @JsonProperty("is_shared")
        @Schema(description = "커뮤니티 업로드 여부", example = "true")
        private Boolean isShared;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커밋 요약 정보")
    public static class CommitSummary {
        @Schema(description = "커밋 SHA", example = "321e1e83793d0b8b933befa9ad08b1ac2d150583")
        private String sha;

        @JsonProperty("commit_message")
        @Schema(description = "커밋 메시지", example = "feat: 로그인 기능 구현")
        private String message;
    }
}
package com.youtil.Api.Community.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

public class CommunityResponseDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "최신 TIL 목록 응답")
    public static class RecentTilListResponse {
        @Schema(description = "최신 TIL 목록")
        private List<RecentTilItem> tils;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "최신 TIL 항목")
    public static class RecentTilItem {
        @Schema(description = "TIL ID", example = "1")
        private Long id;

        @Schema(description = "작성자 ID", example = "1")
        private Long userId;

        @Schema(description = "작성자 닉네임", example = "개발자1")
        private String nickname;

        @Schema(description = "작성자 프로필 이미지 URL")
        private String profileImageUrl;

        @Schema(description = "TIL 제목", example = "스프링부트 시큐리티 설정 방법")
        private String title;

        @Schema(description = "TIL 카테고리", example = "BACKEND")
        private String category;

        @Schema(description = "TIL 태그", example = "[\"Spring\", \"Security\", \"JWT\"]")
        private List<String> tags;

        @Schema(description = "추천 수", example = "5")
        private Integer recommendCount;

        @Schema(description = "조회 수", example = "42")
        private Integer visitedCount;

        @Schema(description = "댓글 수", example = "3")
        private Integer commentsCount;

        @Schema(description = "생성 시간")
        private OffsetDateTime createdAt;
    }
}
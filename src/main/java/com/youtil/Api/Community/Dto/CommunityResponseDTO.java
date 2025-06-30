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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커뮤니티 TIL 목록 응답")
    public static class CommunityTilListResponse {
        @Schema(description = "커뮤니티 TIL 목록")
        private List<CommunityTilItem> tils;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커뮤니티 TIL 항목")
    public static class CommunityTilItem {
        @Schema(description = "TIL ID", example = "1")
        private Long tilId;

        @Schema(description = "사용자 ID", example = "12")
        private Long userId;

        @Schema(description = "사용자명", example = "userName")
        private String useName;

        @Schema(description = "카테고리", example = "FULLSTACK",
                allowableValues = {"FULLSTACK", "AI", "CLOUD"})
        private String category;

        @Schema(description = "TIL 제목", example = "스프링부트 이슈 해결")
        private String title;

        @Schema(description = "작성자", example = "user12")
        private String author;

        @Schema(description = "생성 시간", example = "2025-12-12T18:30:34")
        private String createdAt;

        @Schema(description = "태그 목록", example = "[\"springboot\", \"java\"]")
        private List<String> tags;

        @Schema(description = "추천 수", example = "5")
        private Integer recommend_count;

        @Schema(description = "조회 수", example = "24")
        private Integer visited_count;

        @Schema(description = "댓글 수", example = "4")
        private Integer comments_count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커뮤니티 게시글 상세 응답")
    public static class CommunityPostDetailResponse {
        @Schema(description = "게시물 ID", example = "1")
        private Long postId;

        @Schema(description = "게시물 제목", example = "오늘 TIL 공유합니다")
        private String title;

        @Schema(description = "게시물 내용", example = "오늘은 Spring Security 공부했습니다.")
        private String content;

        @Schema(description = "작성자", example = "user123")
        private String author;

        @Schema(description = "태그 목록", example = "[\"Spring\", \"Security\", \"TIL\"]")
        private List<String> tags;

        @Schema(description = "생성 시간", example = "2025-04-18T14:33:00")
        private String createdAt;

        @Schema(description = "좋아요 수", example = "12")
        private Integer recommend_count;

        @Schema(description = "조회 수", example = "24")
        private Integer visited_count;

        @Schema(description = "댓글 수", example = "10")
        private Integer comments_count;

    }
}

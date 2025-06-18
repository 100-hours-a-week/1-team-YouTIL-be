package com.youtil.Api.Github.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CommitSummaryResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "GitHub 커밋 요약 응답 DTO")
    public static class CommitSummaryResponse {
        @Schema(description = "사용자 이름", example = "ConconDev")
        private String username;

        @Schema(description = "조회 날짜", example = "2024-08-20")
        private String date;

        @Schema(description = "레포지토리 이름", example = "backend")
        private String repo;

        @Schema(description = "레포지토리 소유자", example = "youtil-org")
        private String owner;

        @Schema(description = "커밋 요약 정보 목록")
        private List<CommitSummary> commits;

        @Schema(description = "현재 페이지", example = "1")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "20")
        private int pageSize;

        @Schema(description = "현재 페이지 항목 수", example = "15")
        private int currentPageSize;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private boolean hasNext;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "커밋 요약 정보")
    public static class CommitSummary {
        @Schema(description = "커밋 SHA", example = "3a7bd3e5a0a9d7a1f98b2c3d4e5f6g7h8i9j0k1l2")
        private String sha;

        @Schema(description = "커밋 메시지", example = "fix: 로그인 에러 수정")
        @JsonProperty("commit_message")
        private String commitMessage;
    }
}

package com.youtil.Api.Github.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

public class CommitCalendarResponseDTO {
    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "커밋 존재 여부 달력 응답 DTO")
    public static class CommitCalendarResponse {
        @Schema(description = "사용자명", example = "user_name")
        private String username;

        @Schema(description = "레포지토리명", example = "backend")
        private String repo;

        @Schema(description = "레포지토리 소유자", example = "youtil-org")
        private String owner;

        @Schema(description = "브랜치명", example = "main")
        private String branch;

        @Schema(description = "커밋이 있는 날짜 맵 (날짜: 1)",
                example = "{'2024-06-01': 1, '2024-06-03': 1, '2024-06-10': 1}")
        private Map<String, Integer> calendar;

        @Schema(description = "조회 기간 정보")
        private PeriodInfo period;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "커밋 존재 여부 달력 응답 DTO")
    public static class PeriodInfo {
        @Schema(description = "시작날짜", example = "2025-01-01")
        private String startDate;

        @Schema(description = "종료 날짜", example = "2025-03-31")
        private String endDate;

        @Schema(description = "총 조회 일수", example = "93")
        private int totalDays;

        @Schema(description = "커밋이 있는 일수", example = "32")
        private int commitDays;
    }
}

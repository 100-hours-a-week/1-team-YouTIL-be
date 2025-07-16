package com.youtil.Api.Tils.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TilResponseDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "TIL 생성 응답")
    public static class CreateTilResponse {

        @Schema(description = "생성된 TIL ID", example = "1")
        private Long tilID;
    }

    @Builder
    @Getter
    public static class CreateRequestId {

        private String requestId;
    }

    @Builder
    @Getter
    public static class TilStatus {

        private String status;
        private int position;
        private Long total;
        private String requestId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TilDetailResponse {

        private Long id;
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private String title;
        private String content;
        private String category;
        private List<String> tag;
        private Boolean isDisplay;
        private String commitRepository;
        private Boolean isUploaded;
        private Integer recommendCount;
        private Integer visitedCount;
        private Integer commentsCount;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TilListResponse {

        private List<com.youtil.Api.User.Dto.UserResponseDTO.TilListItem> tils;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "TIL 일괄 삭제 응답")
    public static class BatchDeleteResponse {

        @Schema(description = "성공적으로 삭제된 TIL ID 목록")
        private List<Long> deletedTilIds;

        @Schema(description = "삭제 실패한 TIL ID 목록")
        private List<Long> failedTilIds;

        @Schema(description = "삭제 실패 사유")
        private List<String> failureReasons;

        @Schema(description = "총 요청된 TIL 개수")
        private int totalRequested;

        @Schema(description = "성공적으로 삭제된 TIL 개수")
        private int successCount;

        @Schema(description = "삭제 실패한 TIL 개수")
        private int failureCount;
    }

    @Getter
    @Builder
    public static class GetTilCountResponse {

        @Schema(description = "연도", example = "2025")
        private int year;
        @Schema(description = "TIL 작성 정보 정보")
        private TilRecordYearsItem tils;
    }

    @Getter
    @Builder
    public static class TilRecordYearsItem {

        @Schema(description = "1월", example = "[0,0,0,0]")
        private List<Integer> jan;
        @Schema(description = "2월", example = "[0,0,0,0]")
        private List<Integer> feb;
        @Schema(description = "3월", example = "[0,0,0,0]")
        private List<Integer> mar;
        @Schema(description = "4월", example = "[0,0,0,0]")
        private List<Integer> apr;
        @Schema(description = "5월", example = "[0,0,0,0]")
        private List<Integer> may;
        @Schema(description = "6월", example = "[0,0,0,0]")
        private List<Integer> jun;
        @Schema(description = "7월", example = "[0,0,0,0]")
        private List<Integer> jul;
        @Schema(description = "8월", example = "[0,0,0,0]")
        private List<Integer> aug;
        @Schema(description = "9월", example = "[0,0,0,0]")
        private List<Integer> sep;
        @Schema(description = "10월", example = "[0,0,0,0]")
        private List<Integer> oct;
        @Schema(description = "11월", example = "[0,0,0,0]")
        private List<Integer> nov;
        @Schema(description = "12월", example = "[0,0,0,0]")
        private List<Integer> dec;

    }
}

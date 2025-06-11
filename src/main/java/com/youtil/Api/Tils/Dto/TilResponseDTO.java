package com.youtil.Api.Tils.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

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
}

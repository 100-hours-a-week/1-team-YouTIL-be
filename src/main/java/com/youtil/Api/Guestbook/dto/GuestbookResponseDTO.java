package com.youtil.Api.Guestbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

public class GuestbookResponseDTO {

    @Getter
    @Builder
    @Schema(description = "방명록 작성 성공 응답")
    public static class CreateGuestbookResponseDTO {

        @Schema(description = "생성된 방명록 ID")
        private Long guestbookId;
    }

    @Getter
    @Builder
    @Schema(description = "방명록 리스트 조회 응답")
    public static class GetGuestbookListResponseDTO {

        @Schema(description = "방명록 리스트")
        private List<GuestbookItem> guestbooks;

        @Schema(description = "전체 방명록 개수")
        private long totalCount;

        @Schema(description = "현재 페이지")
        private int currentPage;

        @Schema(description = "페이지 크기")
        private int pageSize;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "방명록 아이템")
    public static class GuestbookItem {

        @Schema(description = "방명록 ID")
        private Long id;

        @Schema(description = "작성자 ID")
        private Long guestId;

        @Schema(description = "작성자 닉네임")
        private String guestNickname;

        @Schema(description = "작성자 프로필 이미지 URL")
        private String guestProfileImageUrl;

        @Schema(description = "방명록 내용")
        private String content;

        @Schema(description = "상위 방명록 ID (답글인 경우)")
        private Long topGuestbookId;

        @Schema(description = "작성 일시")
        private OffsetDateTime createdAt;

        @Schema(description = "수정 일시")
        private OffsetDateTime updatedAt;

        @Schema(description = "삭제된 댓글 여부")
        private boolean deleted;

        @Schema(description = "답글 리스트")
        private List<GuestbookItem> replies;
    }
}

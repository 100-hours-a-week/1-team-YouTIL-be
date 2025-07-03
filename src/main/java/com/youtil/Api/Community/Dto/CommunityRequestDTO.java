package com.youtil.Api.Community.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;

public class CommunityRequestDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커뮤니티 TIL 목록 조회 요청")
    public static class CommunityListRequest {
        @Schema(description = "카테고리 (FULLSTACK, AI,CLOUD)",
                example = "FULLSTACK",
                allowableValues = {"FULLSTACK", "AI","CLOUD"})
        private String category;

        @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
        private Integer page;

        @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
        private Integer offset;
    }
   @Getter
    @Schema(description = "댓글 작성 요청")
    public static class CreateCommentRequest {

        @Schema(description = "댓글 내용", example = "안녕하세요!", maxLength = 50, required = true)
        private String content;

        @Schema(description = "상위 댓글 ID (답글인 경우)", example = "12", required = false)
        private Long topCommentId;
    }

    @Getter
    @Schema(description = "댓글 수정 요청")
    public static class EditCommentRequest {

        @Schema(description = "댓글 내용", example = "안녕하세요!", maxLength = 50, required = true)
        private String content;
    }
}

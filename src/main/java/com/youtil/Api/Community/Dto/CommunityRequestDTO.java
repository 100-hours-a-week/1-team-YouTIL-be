package com.youtil.Api.Community.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

public class CommunityRequestDTO {

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

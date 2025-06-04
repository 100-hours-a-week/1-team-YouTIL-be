package com.youtil.Api.Guestbook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

public class GuestbookRequestDTO {

    @Getter
    @Schema(description = "방명록 작성 요청")
    public static class CreateGuestbookRequestDTO {

        @Schema(description = "방명록 내용", example = "안녕하세요!", maxLength = 50, required = true)
        private String content;

        @Schema(description = "상위 방명록 ID (답글인 경우)", example = "12", required = false)
        private Long topGuestbookId;
    }

    @Getter
    @Schema(description = "방명록 수정 요청")
    public static class UpdateGuestbookRequestDTO {

        @Schema(description = "수정할 방명록 내용", example = "잘보고 갑니다!", maxLength = 50, required = true)
        private String content;
    }
}

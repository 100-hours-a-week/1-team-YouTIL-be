package com.youtil.Api.Tils.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class TilUploadRequestDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "TIL GitHub 업로드 요청")
    public static class UploadRequest {
        @Schema(description = "업로드할 TIL ID", example = "123", required = true)
        private Long tilId;
    }
}

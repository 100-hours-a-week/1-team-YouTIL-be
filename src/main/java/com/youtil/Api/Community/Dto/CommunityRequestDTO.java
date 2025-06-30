package com.youtil.Api.Community.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}

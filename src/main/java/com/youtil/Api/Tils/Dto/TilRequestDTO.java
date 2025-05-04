package com.youtil.Api.Tils.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class TilRequestDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "TIL 생성 요청")
    public static class CreateTilRequest {
        @Schema(description = "GitHub 레포지토리명", example = "Translator_Practice")
        private String repo;

        @Schema(description = "TIL 제목", example = "title")
        private String title;

        @Schema(description = "TIL 카테고리", example = "FULLSTACK")
        private String category;

        @JsonProperty("is_shared")
        @Schema(description = "커뮤니티 업로드 여부 (TRUE: 업로드, FALSE: 미업로드)", example = "true")
        private Boolean isShared;

        @Schema(description = "커밋 정보")
        private List<CommitItem> commits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커밋 정보")
    public static class CommitItem {
        @Schema(description = "커밋 SHA", example = "321e1e83793d0b8b933befa9ad08b1ac2d150583")
        private String sha;

        @Schema(description = "커밋 메시지", example = "merging")
        private String message;

        @Schema(description = "변경 사항 목록")
        private List<ChangeItem> changes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "파일 변경 정보")
    public static class ChangeItem {
        @Schema(description = "파일 경로", example = "src/main/java/com/sookmyung/concon/Review/service/ReviewService.java")
        private String filename;

        @Schema(description = "패치 내용", example = "@@ -23,6 +23,7 @@ public class ReviewService {\\n")
        private String patch;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTilRequest {
        private String title;
        private String content;
        private String category;
        private List<String> tag;
        private Boolean isDisplay;
        private String commitRepository;
        private Boolean isUploaded;
    }
}
package com.youtil.Api.Github.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class CommitDetailResponseDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "선택된 커밋의 상세 정보 응답")
    public static class CommitDetailResponse {
        @Schema(description = "GitHub 사용자명", example = "ConconDev")
        private String username;

        @Schema(description = "커밋 날짜", example = "2024-08-20")
        private String date;

        @Schema(description = "GitHub 레포지토리명", example = "backend")
        private String repo;

        @Schema(description = "파일 상세 정보 목록")
        private List<FileDetail> files;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "파일 변경 상세 정보")
    public static class FileDetail {
        @Schema(description = "파일 경로", example = "src/main/java/com/example/Main.java")
        private String filepath;

        @Schema(description = "파일의 최신 내용")
        private String latest_code;

        @Schema(description = "파일 변경 이력")
        private List<PatchDetail> patches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "패치 상세 정보")
    public static class PatchDetail {
        @Schema(description = "커밋 메시지", example = "feat: 로그인 기능 구현")
        private String commit_message;

        @Schema(description = "변경 내용(패치)", example = "@@ -10,7 +10,15 @@\n import java.util.*;\n...")
        private String patch;
    }
}
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
        @Schema(description = "GitHub 레포지토리 소유자", example = "ConconDev")
        private String repoOwner;

        @Schema(description = "GitHub 레포지토리명", example = "backend")
        private String repo;

        @Schema(description = "GitHub 브랜치명", example = "master")
        private String branch;

        @Schema(description = "커밋 상세 정보 목록")
        private List<CommitDetail> commits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "커밋 상세 정보")
    public static class CommitDetail {
        @Schema(description = "커밋 SHA", example = "3a7bd3e5a0a9d7a1f98b2c3d4e5f6g7h8i9j0k1l2")
        private String sha;

        @Schema(description = "커밋 메시지", example = "feat: 소셜 로그인 추가")
        private String message;

        @Schema(description = "작성자 이름", example = "John Doe")
        private String authorName;

        @Schema(description = "작성자 이메일", example = "john@example.com")
        private String authorEmail;

        @Schema(description = "작성 일시", example = "2024-08-20T14:30:00Z")
        private String authorDate;

        @Schema(description = "변경된 파일 목록")
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

        @Schema(description = "변경 내용(패치)", example = "@@ -10,7 +10,15 @@\n import java.util.*;\n...")
        private String patch;

        @Schema(description = "파일의 최신 내용")
        private String latestCode;
    }
}
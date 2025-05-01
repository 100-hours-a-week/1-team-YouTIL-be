package com.youtil.Api.Github.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "GitHub 커밋 상세 응답 DTO")
public class CommitResponseDTO {

    @Schema(description = "사용자 이름", example = "ConconDev")
    private String username;

    @Schema(description = "조회 날짜", example = "2024-08-20")
    private String date;

    @Schema(description = "레포지토리 이름", example = "backend")
    private String repo;

    @Schema(description = "파일 변경 정보 목록")
    private List<FileInfo> files;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "파일 정보")
    public static class FileInfo {
        @Schema(description = "파일 경로", example = "src/main/java/com/example/User.java")
        private String filepath;

        @Schema(description = "최신 파일 코드", example = "public class User { ... }")
        @JsonProperty("latest_code")
        private String latestCode;

        @Schema(description = "커밋 히스토리 목록")
        private List<CommitInfo> patches;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "커밋 정보")
    public static class CommitInfo {
        @Schema(description = "커밋 메시지", example = "fix: 로그인 에러 수정")
        @JsonProperty("commit_message")
        private String commitMessage;

        @Schema(description = "패치 코드", example = "@@ -1,5 +1,5 @@ ...")
        private String patch;
    }
}
package com.youtil.Api.Github.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "깃허브 커밋 응답 DTO")
public class CommitResponseDTO {

    @Schema(description = "커밋 목록")
    private List<CommitItem> commits;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "커밋 단일 항목")
    public static class CommitItem {
        @Schema(description = "커밋 SHA", example = "abc123def456")
        private String sha;

        @Schema(description = "커밋 메시지", example = "Fix login bug")
        private String message;

        @Schema(description = "파일 변경 내역 목록")
        private List<ChangeItem> changes;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "파일 변경 항목")
    public static class ChangeItem {
        @Schema(description = "파일 이름", example = "src/main/java/com/example/service/UserService.java")
        private String filename;

        @Schema(description = "코드 변경 내용", example = "@@ -12,7 +12,8 @@ public class UserService {")
        private String patch;
    }
}

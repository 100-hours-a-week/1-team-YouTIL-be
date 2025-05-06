package com.youtil.Api.Tils.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TilAiRequestDTO {
    private String username;
    private String date;
    private String repo;
    private List<FileInfo> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String filepath;
        private String latest_code;
        private List<PatchInfo> patches;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatchInfo {
        private String commit_message;
        private String patch;
    }
}

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
    private String repository;
    private String owner;
    private String branch;
    private List<CommitInfo> commits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommitInfo {
        private String sha;
        private String message;
        private String author;
        private String date;
        private List<FileChange> changes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileChange {
        private String filename;
        private String patch;
        private String content;
    }
}
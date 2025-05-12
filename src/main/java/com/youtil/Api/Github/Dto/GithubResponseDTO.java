package com.youtil.Api.Github.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class GithubResponseDTO {

    @Getter
    @Builder
    @Schema(description = "깃허브 조직 응답")
    public static class OrganizationResponseDTO {
        @Schema(description = "조직 목록")
        private List<OrganizationItem> organizations;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "깃허브 조직 정보")
    public static class OrganizationItem {
        @Schema(description = "조직 ID", example = "123")
        private Long organization_id;

        @Schema(description = "조직 이름", example = "카카오")
        private String organization_name;
    }

    @Getter
    @Builder
    @Schema(description = "깃허브 레포지토리 응답")
    public static class RepositoryResponseDTO {
        @Schema(description = "레포지토리 목록")
        private List<RepositoryItem> repositories;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "깃허브 레포지토리 정보")
    public static class RepositoryItem {
        @Schema(description = "레포지토리 ID", example = "12")
        private Long repositoryId;

        @Schema(description = "레포지토리 이름", example = "백엔드")
        private String repositoryName;
    }

    @Getter
    @Builder
    @Schema(description = "깃허브 브랜치 응답")
    public static class BranchResponseDTO {
        @Schema(description = "브랜치 목록")
        private List<BranchItem> branches;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "깃허브 브랜치 정보")
    public static class BranchItem {
        @Schema(description = "브랜치 이름", example = "main")
        private String name;
    }
}
package com.youtil.Api.Github.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class GithubResponseDTO {

    // 페이지네이션 정보 클래스는 내부적으로 사용하므로 유지
    @Getter
    @Builder
    @Schema(description = "깃허브 API 페이지네이션 정보")
    public static class PaginationInfo {
        @Schema(description = "현재 페이지", example = "1")
        private int currentPage;

        @Schema(description = "페이지당 항목 수", example = "30")
        private int size;

        @Schema(description = "전체 항목 수", example = "120")
        private int totalItems;

        @Schema(description = "전체 페이지 수", example = "4")
        private int totalPages;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private boolean hasNext;
    }

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
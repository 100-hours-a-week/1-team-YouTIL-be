package com.youtil.Api.Github.Converter;

import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Dto.GithubResponseDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GitHub API 응답을 DTO로 변환하는 Converter 클래스
 */
public class GitHubDtoConverter {

    /**
     * GitHub API 조직 응답을 OrganizationResponseDTO로 변환
     */
    public static GithubResponseDTO.OrganizationResponseDTO toOrganizationResponse(
            Map<String, Object>[] orgsResponse, int currentPage, int pageSize) {

        List<GithubResponseDTO.OrganizationItem> organizations = new ArrayList<>();

        if (orgsResponse != null) {
            organizations = java.util.Arrays.stream(orgsResponse)
                    .map(org -> new GithubResponseDTO.OrganizationItem(
                            Long.valueOf(org.get("id").toString()),
                            org.get("login").toString()))
                    .collect(Collectors.toList());
        }

        boolean hasNext = organizations.size() == pageSize;

        return GithubResponseDTO.OrganizationResponseDTO.builder()
                .organizations(organizations)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .currentPageSize(organizations.size())
                .hasNext(hasNext)
                .build();
    }

    /**
     * GitHub API 레포지토리 응답을 RepositoryResponseDTO로 변환
     */
    public static GithubResponseDTO.RepositoryResponseDTO toRepositoryResponse(
            Map<String, Object>[] reposResponse, int currentPage, int pageSize) {

        List<GithubResponseDTO.RepositoryItem> repositories = new ArrayList<>();

        if (reposResponse != null) {
            repositories = java.util.Arrays.stream(reposResponse)
                    .map(repo -> new GithubResponseDTO.RepositoryItem(
                            Long.valueOf(repo.get("id").toString()),
                            repo.get("name").toString()))
                    .collect(Collectors.toList());
        }

        // 다음 페이지 존재 여부 판단
        boolean hasNext = repositories.size() == pageSize;

        return GithubResponseDTO.RepositoryResponseDTO.builder()
                .repositories(repositories)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .currentPageSize(repositories.size())
                .hasNext(hasNext)
                .build();
    }

    /**
     * GitHub API 브랜치 응답을 BranchResponseDTO로 변환
     */
    public static GithubResponseDTO.BranchResponseDTO toBranchResponse(
            Map<String, Object>[] branchesResponse, int currentPage, int pageSize) {

        List<GithubResponseDTO.BranchItem> branches = new ArrayList<>();

        if (branchesResponse != null) {
            branches = java.util.Arrays.stream(branchesResponse)
                    .map(branch -> new GithubResponseDTO.BranchItem(branch.get("name").toString()))
                    .collect(Collectors.toList());
        }

        // 다음 페이지 존재 여부 판단
        boolean hasNext = branches.size() == pageSize;

        return GithubResponseDTO.BranchResponseDTO.builder()
                .branches(branches)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .currentPageSize(branches.size())
                .hasNext(hasNext)
                .build();
    }

    /**
     * GitHub API 커밋 상세 응답을 CommitDetailResponse로 변환
     */
    public static CommitDetailResponseDTO.CommitDetailResponse toCommitDetailResponse(
            List<CommitDetailResponseDTO.FileDetail> fileDetails,
            String username,
            String date,
            String repoName) {

        return CommitDetailResponseDTO.CommitDetailResponse.builder()
                .username(username)
                .date(date)
                .repo(repoName)
                .files(fileDetails)
                .build();
    }

    /**
     * TilRequestDTO.CommitSummary를 CommitDetailRequestDTO.CommitSummary로 변환
     */
    public static List<CommitDetailRequestDTO.CommitSummary> toCommitDetailRequestSummaries(
            List<com.youtil.Api.Tils.Dto.TilRequestDTO.CommitSummary> tilCommits) {

        if (tilCommits == null) {
            return new ArrayList<>();
        }

        return tilCommits.stream()
                .map(commit -> CommitDetailRequestDTO.CommitSummary.builder()
                        .sha(commit.getSha())
                        .message(commit.getMessage())
                        .build())
                .collect(Collectors.toList());
    }
}

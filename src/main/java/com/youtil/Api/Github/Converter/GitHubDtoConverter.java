package com.youtil.Api.Github.Converter;

import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Api.Github.Dto.GithubResponseDTO;

import java.time.OffsetDateTime;
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
            Map<String, Object>[] orgsResponse) {

        List<GithubResponseDTO.OrganizationItem> organizations = new ArrayList<>();

        if (orgsResponse != null) {
            organizations = java.util.Arrays.stream(orgsResponse)
                    .map(org -> new GithubResponseDTO.OrganizationItem(
                            Long.valueOf(org.get("id").toString()),
                            org.get("login").toString()))
                    .collect(Collectors.toList());
        }

        return GithubResponseDTO.OrganizationResponseDTO.builder()
                .organizations(organizations)
                .build();
    }

    /**
     * GitHub API 레포지토리 응답을 RepositoryResponseDTO로 변환
     */
    public static GithubResponseDTO.RepositoryResponseDTO toRepositoryResponse(
            Map<String, Object>[] reposResponse) {

        List<GithubResponseDTO.RepositoryItem> repositories = new ArrayList<>();

        if (reposResponse != null) {
            repositories = java.util.Arrays.stream(reposResponse)
                    .map(repo -> new GithubResponseDTO.RepositoryItem(
                            Long.valueOf(repo.get("id").toString()),
                            repo.get("name").toString()))
                    .collect(Collectors.toList());
        }

        return GithubResponseDTO.RepositoryResponseDTO.builder()
                .repositories(repositories)
                .build();
    }

    /**
     * GitHub API 브랜치 응답을 BranchResponseDTO로 변환
     */
    public static GithubResponseDTO.BranchResponseDTO toBranchResponse(
            Map<String, Object>[] branchesResponse) {

        List<GithubResponseDTO.BranchItem> branches = new ArrayList<>();

        if (branchesResponse != null) {
            branches = java.util.Arrays.stream(branchesResponse)
                    .map(branch -> new GithubResponseDTO.BranchItem(branch.get("name").toString()))
                    .collect(Collectors.toList());
        }

        return GithubResponseDTO.BranchResponseDTO.builder()
                .branches(branches)
                .build();
    }

    /**
     * GitHub API 커밋 응답을 CommitSummaryResponse로 변환
     */
    public static CommitSummaryResponseDTO.CommitSummaryResponse toCommitSummaryResponse(
            Map<String, Object>[] commitsResponse,
            String username,
            String date,
            String repoName,
            String owner) {

        List<CommitSummaryResponseDTO.CommitSummary> commitSummaries = new ArrayList<>();

        if (commitsResponse != null) {
            commitSummaries = java.util.Arrays.stream(commitsResponse)
                    .map(commit -> {
                        String sha = commit.get("sha").toString();
                        Map<String, Object> commitData = (Map<String, Object>) commit.get("commit");
                        String message = commitData.get("message").toString();

                        return CommitSummaryResponseDTO.CommitSummary.builder()
                                .sha(sha)
                                .commitMessage(message)
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                .username(username)
                .date(date)
                .repo(repoName)
                .owner(owner)
                .commits(commitSummaries)
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
     * 커밋 API 응답에서 날짜 추출
     */
    public static String extractDateFromCommit(Map<String, Object> commitInfo) {
        try {
            Map<String, Object> commit = (Map<String, Object>) commitInfo.get("commit");
            if (commit != null && commit.containsKey("committer")) {
                Map<String, Object> committer = (Map<String, Object>) commit.get("committer");
                if (committer != null && committer.containsKey("date")) {
                    String dateStr = committer.get("date").toString();
                    OffsetDateTime dateTime = OffsetDateTime.parse(dateStr);
                    return dateTime.toLocalDate().toString();
                }
            }
        } catch (Exception e) {
            // 날짜 파싱 실패시 null 반환
        }
        return null;
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
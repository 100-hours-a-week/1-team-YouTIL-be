package com.youtil.Api.Github.Converter;

import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Api.Github.Dto.CommitCalendarResponseDTO;

import java.time.LocalDate;
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
            Map<String, Object>[] orgsResponse, int currentPage, int pageSize) {

        List<GithubResponseDTO.OrganizationItem> organizations = new ArrayList<>();

        if (orgsResponse != null) {
            organizations = java.util.Arrays.stream(orgsResponse)
                    .map(org -> new GithubResponseDTO.OrganizationItem(
                            Long.valueOf(org.get("id").toString()),
                            org.get("login").toString()))
                    .collect(Collectors.toList());
        }

        // 다음 페이지 존재 여부 판단: 요청한 pageSize와 실제 받은 데이터 수가 같으면 다음 페이지가 있을 가능성
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
     * 기존 호환성을 위한 오버로드 메서드들 (페이지네이션 정보 없이 - 기본값 사용)
     */
    public static GithubResponseDTO.OrganizationResponseDTO toOrganizationResponse(
            Map<String, Object>[] orgsResponse) {
        int defaultSize = orgsResponse != null ? orgsResponse.length : 0;
        return toOrganizationResponse(orgsResponse, 1, defaultSize);
    }

    public static GithubResponseDTO.RepositoryResponseDTO toRepositoryResponse(
            Map<String, Object>[] reposResponse) {
        int defaultSize = reposResponse != null ? reposResponse.length : 0;
        return toRepositoryResponse(reposResponse, 1, defaultSize);
    }

    public static GithubResponseDTO.BranchResponseDTO toBranchResponse(
            Map<String, Object>[] branchesResponse) {
        int defaultSize = branchesResponse != null ? branchesResponse.length : 0;
        return toBranchResponse(branchesResponse, 1, defaultSize);
    }

    /**
     * GitHub API 커밋 응답을 CommitSummaryResponse로 변환
     */
    public static CommitSummaryResponseDTO.CommitSummaryResponse toCommitSummaryResponse(
            Map<String, Object>[] commitsResponse, String username,String date,String repoName,String owner,
            int currentPage, int pageSize) {

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

        boolean hasNext = commitSummaries.size() == pageSize;

        return CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                .username(username)
                .date(date)
                .repo(repoName)
                .owner(owner)
                .commits(commitSummaries)
                .currentPage(currentPage)
                .pageSize(pageSize)
                .currentPageSize(commitSummaries.size())
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

    /**
     * 커밋 달력 응답을 생성합니다.
     */
    public static CommitCalendarResponseDTO.CommitCalendarResponse toCommitCalendarResponse(
            String username, String repo, String owner, String branch,
            Map<String, Integer> calendar, String startDate, String endDate) {

        CommitCalendarResponseDTO.PeriodInfo periodInfo = CommitCalendarResponseDTO.PeriodInfo.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalDays(calculateDaysBetween(startDate, endDate))
                .commitDays(calendar.size())
                .build();

        return CommitCalendarResponseDTO.CommitCalendarResponse.builder()
                .username(username)
                .repo(repo)
                .owner(owner)
                .branch(branch)
                .calendar(calendar)
                .period(periodInfo)
                .build();
    }

    /**
     * 두 날짜 사이의 일수를 계산합니다.
     */
    private static int calculateDaysBetween(String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        } catch (Exception e) {
            return 0;
        }
    }
}

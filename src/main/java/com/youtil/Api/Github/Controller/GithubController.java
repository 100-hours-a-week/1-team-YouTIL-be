package com.youtil.Api.Github.Controller;

import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Api.Github.Service.GithubService;
import com.youtil.Common.ApiResponse;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "github", description = "깃허브 관련 API")
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
public class GithubController {

    private final GithubService githubService;

    @Operation(summary = "깃허브 조직 목록 조회", description = "사용자의 깃허브 조직 목록을 조회하는 API입니다.")
    @GetMapping("/organization")
    public ApiResponse<GithubResponseDTO.OrganizationResponseDTO> getOrganizations() {
        Long userId = JwtUtil.getAuthenticatedUserId();
        return new ApiResponse<>("깃허브 조직 목록 조회에 성공했습니다.", "200",
                githubService.getOrganizations(userId));
    }

    @Operation(summary = "깃허브 브랜치 목록 조회", description = "조직 ID가 있으면 해당 조직의 브랜치를, 없으면 개인 레포지토리의 브랜치를 조회합니다.")
    @GetMapping("/branches")
    public ApiResponse<GithubResponseDTO.BranchResponseDTO> getBranches(
            @Parameter(name = "organizationId", description = "조직 ID", required = false)
            @RequestParam(required = false) Long organizationId,
            @Parameter(name = "repositoryId", description = "레포지토리 ID", required = true)
            @RequestParam Long repositoryId) {

        Long userId = JwtUtil.getAuthenticatedUserId();

        if (organizationId != null) {
            return new ApiResponse<>("조직 레포지토리 브랜치 목록 조회 성공", "200",
                    githubService.getBranchesByRepositoryId(userId, organizationId, repositoryId));
        } else {
            return new ApiResponse<>("개인 레포지토리 브랜치 목록 조회 성공", "200",
                    githubService.getBranchesByRepositoryIdWithoutOrg(userId, repositoryId));
        }
    }

    @Operation(summary = "깃허브 레포지토리 목록 조회", description = "특정 조직의 레포지토리 목록을 조회하는 API입니다.")
    @GetMapping("/repositories")
    public ApiResponse<GithubResponseDTO.RepositoryResponseDTO> getRepositories(
            @Parameter(name = "organizationId", description = "조직 ID", required = false)
            @RequestParam(required = false) Long organizationId) {

        Long userId = JwtUtil.getAuthenticatedUserId();

        if (organizationId != null) {
            return new ApiResponse<>("조직 레포지토리 목록 조회 성공", "200",
                    githubService.getRepositoriesByOrganizationId(userId, organizationId));
        } else {
            return new ApiResponse<>("개인 레포지토리 목록 조회 성공", "200",
                    githubService.getUserRepositories(userId));
        }
    }
}
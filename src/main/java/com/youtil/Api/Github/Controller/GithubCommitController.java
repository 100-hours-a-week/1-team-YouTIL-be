package com.youtil.Api.Github.Controller;

import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Api.Github.Dto.CommitResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitService;
import com.youtil.Common.ApiResponse;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
public class GithubCommitController {

    private final GithubCommitService githubCommitService;

    @GetMapping("/commits")
    @Operation(summary = "깃허브 커밋 조회", description = "브랜치의 커밋과 파일 변경 내역을 조회합니다.")
    public ApiResponse<CommitResponseDTO> getCommits(
            @RequestParam(required = false) Long organizationId,
            @RequestParam Long repositoryId,
            @RequestParam String branchId,
            @RequestParam String date
    ) {
        Long userId = JwtUtil.getAuthenticatedUserId();
        return new ApiResponse<>("성공했습니다.", "200",
                githubCommitService.getCommits(userId, organizationId, repositoryId, branchId, date));
    }
}

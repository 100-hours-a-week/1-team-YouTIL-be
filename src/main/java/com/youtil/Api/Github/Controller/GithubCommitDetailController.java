package com.youtil.Api.Github.Controller;

import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitDetailService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "github", description = "깃허브 관련 API")
public class GithubCommitDetailController {

    private final GithubCommitDetailService githubCommitDetailService;

    @Operation(
            summary = "선택된 커밋의 상세 정보 조회",
            description = "GitHub API를 통해 선택된 커밋들의 상세 정보(메시지, 파일 변경, 코드 등)를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "커밋 상세 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommitDetailResponseDTO.CommitDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "GitHub API 호출 오류"
            )
    })
    @PostMapping(
            value = "/commits/detail",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<CommitDetailResponseDTO.CommitDetailResponse>> getCommitDetails(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "조회할 커밋 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CommitDetailRequestDTO.CommitDetailRequest.class))
            )
            @RequestBody CommitDetailRequestDTO.CommitDetailRequest request) {

        log.info("커밋 상세 정보 조회 요청: 조직={}, 레포={}, 브랜치={}, 커밋 {}개",
                request.getOrganizationId(), request.getRepositoryId(), request.getBranch(),
                request.getCommits() != null ? request.getCommits().size() : 0);

        try {
            // 요청 검증
            if (request.getRepositoryId() == null) {
                throw new IllegalArgumentException("레포지토리 ID가 필요합니다.");
            }
            if (request.getBranch() == null || request.getBranch().isEmpty()) {
                throw new IllegalArgumentException("브랜치명이 필요합니다.");
            }
            if (request.getCommits() == null || request.getCommits().isEmpty()) {
                throw new IllegalArgumentException("최소 하나 이상의 커밋 정보가 필요합니다.");
            }

            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스 호출
            CommitDetailResponseDTO.CommitDetailResponse response =
                    githubCommitDetailService.getCommitDetails(request, userId);

            log.info("커밋 상세 정보 조회 성공: {}개 커밋 정보 반환",
                    response.getCommits() != null ? response.getCommits().size() : 0);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            TilMessageCode.GITHUB_COMMIT_DETAIL_FETCHED.getMessage(),
                            TilMessageCode.GITHUB_COMMIT_DETAIL_FETCHED.getCode(),
                            response)
            );
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("커밋 상세 정보 조회 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        }
    }
}
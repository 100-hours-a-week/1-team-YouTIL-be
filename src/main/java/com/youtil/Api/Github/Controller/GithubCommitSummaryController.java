package com.youtil.Api.Github.Controller;

import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitSummaryService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "github", description = "깃허브 관련 API")
public class GithubCommitSummaryController {

    private final GithubCommitSummaryService githubCommitSummaryService;

    @GetMapping("/commits")
    @Operation(
            summary = "깃허브 커밋 조회",
            description = "특정 날짜의 커밋 기본 정보(SHA, 메시지)를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "커밋 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommitSummaryResponseDTO.CommitSummaryResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 파라미터"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "커밋 데이터 없음"
            )
    })
    public ApiResponse<CommitSummaryResponseDTO.CommitSummaryResponse> getCommitSummary(
            @RequestParam(required = false) Long organizationId,
            @RequestParam Long repositoryId,
            @RequestParam String branchId,
            @RequestParam String date) {

        log.info("GitHub 커밋 조회 요청: 조직={}, 레포={}, 브랜치={}, 날짜={}",
                organizationId, repositoryId, branchId, date);

        Long userId = JwtUtil.getAuthenticatedUserId();

        try {
            CommitSummaryResponseDTO.CommitSummaryResponse result = githubCommitSummaryService.getCommitSummary(
                    userId, organizationId, repositoryId, branchId, date);

            log.info("GitHub 커밋 조회 성공: {} 개 커밋 정보 반환",
                    result.getCommits() != null ? result.getCommits().size() : 0);

            return new ApiResponse<>(
                    TilMessageCode.GITHUB_COMMITS_FETCHED.getMessage(),
                    TilMessageCode.GITHUB_COMMITS_FETCHED.getCode(),
                    result);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    TilMessageCode.GITHUB_INVALID_DATE_FORMAT.getMessage());
        } catch (RuntimeException e) {
            log.error("커밋 조회 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    TilMessageCode.GITHUB_API_ERROR.getMessage() + ": " + e.getMessage());
        }
    }
}
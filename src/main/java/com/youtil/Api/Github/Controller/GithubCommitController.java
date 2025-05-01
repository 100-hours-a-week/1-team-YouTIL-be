package com.youtil.Api.Github.Controller;

import com.youtil.Api.Github.Dto.CommitResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitService;
import com.youtil.Common.ApiResponse;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class GithubCommitController {

    private final GithubCommitService githubCommitService;

    @GetMapping("/commits")
    @Operation(
            summary = "깃허브 커밋 조회",
            description = "브랜치의 커밋과 파일 변경 내역을 조회합니다. 해당 날짜 이후의 커밋 정보를 파일별로 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "커밋 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommitResponseDTO.class))
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
    public ApiResponse<CommitResponseDTO> getCommit(
            @RequestParam(required = false) Long organizationId,
            @RequestParam Long repositoryId,
            @RequestParam String branchId,
            @RequestParam String date) {

        log.info("GitHub 커밋 조회 요청: 조직={}, 레포={}, 브랜치={}, 날짜={}",
                organizationId, repositoryId, branchId, date);

        Long userId = JwtUtil.getAuthenticatedUserId();

        try {
            CommitResponseDTO result = githubCommitService.getCommits(
                    userId, organizationId, repositoryId, branchId, date);

            log.info("GitHub 커밋 조회 성공: {} 개 파일 정보 반환",
                    result.getFiles() != null ? result.getFiles().size() : 0);

            return new ApiResponse<>("성공했습니다.", "200", result);
        } catch (IllegalArgumentException e) {
            // 잘못된 입력 파라미터 (날짜 형식 등)
            log.warn("잘못된 요청 파라미터: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DateTimeParseException e) {
            // 날짜 파싱 오류
            log.warn("날짜 파싱 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식이어야 합니다.");
        } catch (RuntimeException e) {
            // 기타 런타임 예외
            log.error("커밋 조회 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "GitHub 커밋 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
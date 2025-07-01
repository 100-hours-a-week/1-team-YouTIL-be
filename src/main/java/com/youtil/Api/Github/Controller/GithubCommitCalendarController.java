package com.youtil.Api.Github.Controller;

import com.youtil.Api.Github.Dto.CommitCalendarResponseDTO.CommitCalendarResponse;
import com.youtil.Api.Github.Service.GithubCommitCalendarService;
import com.youtil.Common.ApiResponse;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "github", description = "깃허브 관련 API")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class GithubCommitCalendarController {

    private final GithubCommitCalendarService githubCommitCalendarService;

    @Operation(
            summary = "연도별 커밋 존재 여부 달력 조회",
            description = "사용자가 선택한 연도 전체(1월 1일 ~ 12월 31일)의 커밋이 있는 날짜들을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "커밋 달력 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommitCalendarResponse.class))
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
                    responseCode = "500",
                    description = "GitHub API 호출 오류"
            )
    })
    @GetMapping("/commits/records")
    public ApiResponse<CommitCalendarResponse> getCommitCalendar(
            @Parameter(name = "organizationId", description = "조직 ID (선택사항)", required = false)
            @RequestParam(required = false) Long organizationId,

            @Parameter(name = "repositoryId", description = "레포지토리 ID", required = true, example = "927579728")
            @RequestParam Long repositoryId,

            @Parameter(name = "branchId", description = "브랜치명", required = true, example = "main")
            @RequestParam String branchId,

            @Parameter(name = "year", description = "조회할 연도 (YYYY). 기본값: 현재 연도", required = false, example = "2024")
            @RequestParam(required = false) Integer year) {

        log.info("커밋 달력 조회 요청: 조직={}, 레포={}, 브랜치={}, 연도={}",
                organizationId, repositoryId, branchId, year);

        Long userId = JwtUtil.getAuthenticatedUserId();

        try {
            // 요청 검증
            if (repositoryId == null) {
                throw new IllegalArgumentException("레포지토리 ID는 필수입니다.");
            }
            if (branchId == null || branchId.trim().isEmpty()) {
                throw new IllegalArgumentException("브랜치명은 필수입니다.");
            }

            // 연도 파싱 및 기본값 설정
            LocalDate now = LocalDate.now();
            int targetYear = year != null ? year : now.getYear();

            // 해당 연도 전체 기간 설정
            LocalDate start = LocalDate.of(targetYear, 1, 1); // 연도의 첫 날
            LocalDate end = LocalDate.of(targetYear, 12, 31); // 연도의 마지막 날

            log.info("조회 설정: {}년 전체 조회, 기간: {} ~ {}", targetYear, start, end);

            // 서비스 호출 (해당 연도 전체 조회)
            CommitCalendarResponse result =
                    githubCommitCalendarService.getCommitCalendar(userId, organizationId, repositoryId, branchId, start, end);

            log.info("커밋 달력 조회 성공: {}년 전체 중 {}일에 커밋 존재",
                    targetYear, result.getPeriod().getCommitDays());

            return new ApiResponse<>(
                    String.format("%d년 전체 커밋 달력 조회가 완료되었습니다.", targetYear),
                    "GITHUB_COMMIT_CALENDAR_FETCHED",
                    result);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청 파라미터: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요.");
        } catch (RuntimeException e) {
            log.error("커밋 달력 조회 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "커밋 달력 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

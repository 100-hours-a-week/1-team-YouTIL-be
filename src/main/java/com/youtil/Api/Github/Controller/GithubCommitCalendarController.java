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

@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "github", description = "깃허브 관련 API")
public class GithubCommitCalendarController {

    private final GithubCommitCalendarService githubCommitCalendarService;

    @Operation(
            summary = "커밋 존재 여부 달력 조회",
            description = "지정된 기간 동안 커밋이 있는 날짜들을 조회합니다. Redis 캐싱을 활용하여 성능을 최적화합니다."
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

            @Parameter(name = "startDate", description = "시작 날짜 (YYYY-MM-DD). 기본값: 1년 전", required = false, example = "2024-07-01")
            @RequestParam(required = false) String startDate,

            @Parameter(name = "endDate", description = "종료 날짜 (YYYY-MM-DD). 기본값: 오늘", required = false, example = "2025-06-20")
            @RequestParam(required = false) String endDate) {

        log.info("커밋 달력 조회 요청: 조직={}, 레포={}, 브랜치={}, 시작일={}, 종료일={}",
                organizationId, repositoryId, branchId, startDate, endDate);

        Long userId = JwtUtil.getAuthenticatedUserId();

        try {
            // 요청 검증
            if (repositoryId == null) {
                throw new IllegalArgumentException("레포지토리 ID는 필수입니다.");
            }
            if (branchId == null || branchId.trim().isEmpty()) {
                throw new IllegalArgumentException("브랜치명은 필수입니다.");
            }

            // 날짜 파싱 및 기본값 설정
            LocalDate start, end;

            if (endDate != null) {
                try {
                    end = LocalDate.parse(endDate);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("종료 날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요.");
                }
            } else {
                end = LocalDate.now();
            }

            if (startDate != null) {
                try {
                    start = LocalDate.parse(startDate);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("시작 날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요.");
                }
            } else {
                start = end.minusYears(1); // 기본값: 1년 전
            }

            // 날짜 범위 검증
            if (start.isAfter(end)) {
                throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이전이어야 합니다.");
            }

            // 최대 조회 기간 제한 (1년)
            if (start.isBefore(end.minusYears(1))) {
                throw new IllegalArgumentException("조회 기간은 최대 1년까지 가능합니다.");
            }

            // 서비스 호출
            CommitCalendarResponse result =
                    githubCommitCalendarService.getCommitCalendar(userId, organizationId, repositoryId, branchId, start, end);

            log.info("커밋 달력 조회 성공: {}일 중 {}일에 커밋 존재",
                    result.getPeriod().getTotalDays(), result.getPeriod().getCommitDays());

            return new ApiResponse<>(
                    "커밋 달력 조회가 완료되었습니다.",
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

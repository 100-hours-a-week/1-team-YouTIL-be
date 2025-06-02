package com.youtil.Api.Interview.Controller;

import com.youtil.Api.Interview.Service.InterViewService;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.CreateInterviewResponseDTO;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewsResponse;
import com.youtil.Api.Interview.dto.interviewRequestDTO.CreateInterviewRequest;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.InterviewMessageCode;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/interviews")
@Tag(name = "interviews", description = "면접 질문 관련 API")
@RequiredArgsConstructor
@RestController
public class InterviewController {

    private final InterViewService interViewService;

    @Operation(
            summary = "면접 질문 생성",
            description = " TIL 기반으로 면접 질문을 생성합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "면접 질문 생성 성공",
                    content = @Content(schema = @Schema(implementation = String.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "해당하는 유저가 존재하지 않습니다."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "해당하는 TIL이 존재하지 않습니다."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류 입니다."
            )
    })
    @PostMapping("")
    ResponseEntity<ApiResponse<CreateInterviewResponseDTO>> createInterview(
            @RequestBody CreateInterviewRequest request) {

        //interViewService.createInterview(request, JwtUtil.getAuthenticatedUserId())

        return new ResponseEntity<>(new ApiResponse<>(
                InterviewMessageCode.INTERVIEW_CREATED.getMessage(),
                InterviewMessageCode.INTERVIEW_CREATED.getCode(),
                CreateInterviewResponseDTO.builder().interviewId(1L).build()), HttpStatus.CREATED);
    }

    @GetMapping("")
    ResponseEntity<ApiResponse<GetInterviewsResponse>> getInterviews(
            @RequestParam(value = "date") String dateStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        return ResponseEntity.ok(
                new ApiResponse<>(InterviewMessageCode.FIND_INTERVIEWS_SUCCESS.getMessage(),
                        InterviewMessageCode.FIND_INTERVIEW_SUCCESS.getCode(),
                        interViewService.getInterviews(JwtUtil.getAuthenticatedUserId(), pageable,
                                date)));

    }

    @GetMapping("/{interviewId}")
    ResponseEntity<ApiResponse<GetInterviewResponse>> getInterview(
            @Parameter(name = "interviewId", description = "조회하고자 하는 면접질문 아이디입니다.", required = true, example = "1")
            @PathVariable Long interviewId
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(InterviewMessageCode.FIND_INTERVIEW_SUCCESS.getMessage(),
                        InterviewMessageCode.FIND_INTERVIEWS_SUCCESS.getCode(),
                        interViewService.getInterview(interviewId)));

    }

    @DeleteMapping("/{interviewId}")
    ResponseEntity<ApiResponse<String>> deleteInterview(
            @Parameter(name = "interviewId", description = "조회하고자 하는 면접질문 아이디입니다.", required = true, example = "1")
            @PathVariable Long interviewId
    ) {

        interViewService.inactivateInterview(JwtUtil.getAuthenticatedUserId(), interviewId);

        return ResponseEntity.ok(
                new ApiResponse<>(InterviewMessageCode.INTERVIEW_INACTIVATE_SUCCESS.getMessage(),
                        InterviewMessageCode.INTERVIEW_INACTIVATE_SUCCESS.getCode()));

    }
}

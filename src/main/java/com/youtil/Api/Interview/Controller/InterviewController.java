package com.youtil.Api.Interview.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Interview.Queue.InterviewQueueProducer;
import com.youtil.Api.Interview.Service.InterViewService;
import com.youtil.Api.Interview.dto.InterviewRequestDTO.CreateInterviewRequest;
import com.youtil.Api.Interview.dto.InterviewRequestDTO.InactiveInterviewRequest;
import com.youtil.Api.Interview.dto.InterviewResponseDTO;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.CreateInterviewResponseDTO;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewCountResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewResponse;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.GetInterviewsResponse;
import com.youtil.Common.ApiResponse;
import static com.youtil.Common.Constants.InterviewServiceConstans.RESEND_TIMEOUT_SECONDS;
import static com.youtil.Common.Constants.InterviewServiceConstans.RESULT_KEY;
import com.youtil.Common.Enums.InterviewMessageCode;
import static com.youtil.Common.Enums.InterviewMessageCode.INTERVIEW_RECORD_SUCCESS;
import com.youtil.Exception.InterviewException.InterviewException;
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
import org.springframework.data.redis.core.StringRedisTemplate;
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

@RequestMapping("/api/v1/interviews")
@Tag(name = "interviews", description = "면접 질문 관련 API")
@RequiredArgsConstructor
@RestController
public class InterviewController {

    private final InterViewService interViewService;
    private final InterviewQueueProducer interviewQueueProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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
            @RequestBody CreateInterviewRequest request) throws Exception {

        Long userId = JwtUtil.getAuthenticatedUserId();

        String requestId = interviewQueueProducer.enqueueInterviewRequest(userId, request);
        String resultKey = RESULT_KEY + requestId;

        CreateInterviewResponseDTO response = waitForResult(resultKey,
                RESEND_TIMEOUT_SECONDS);

        return new ResponseEntity<>(new ApiResponse<>(
                InterviewMessageCode.INTERVIEW_CREATED.getMessage(),
                InterviewMessageCode.INTERVIEW_CREATED.getCode(),
                response), HttpStatus.CREATED);
//        return new ResponseEntity<>(new ApiResponse<>(
//                InterviewMessageCode.INTERVIEW_CREATED.getMessage(),
//                InterviewMessageCode.INTERVIEW_CREATED.getCode(),
//                CreateInterviewResponseDTO.builder().interviewId(1L).build()), HttpStatus.CREATED);
    }

    @Operation(
            summary = "면접 질문리스트 조회",
            description = " 면접 질문 리스트를 조회합니다."
    )
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

    @Operation(
            summary = "면접 질문 상세 조회",
            description = " 면접 질문 상세 정보를 조회합니다."
    )
    @GetMapping("/{interviewId:\\d+}")
    ResponseEntity<ApiResponse<GetInterviewResponse>> getInterview(
            @Parameter(name = "interviewId", description = "조회하고자 하는 면접질문 아이디입니다.", required = true, example = "1")
            @PathVariable Long interviewId
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(InterviewMessageCode.FIND_INTERVIEW_SUCCESS.getMessage(),
                        InterviewMessageCode.FIND_INTERVIEWS_SUCCESS.getCode(),
                        interViewService.getInterview(interviewId)));

    }

    @Operation(
            summary = "면접 질문 삭제",
            description = " 면접 질문을 삭제합니다."
    )
    @DeleteMapping("")
    ResponseEntity<ApiResponse<String>> deleteInterview(
            @RequestBody InactiveInterviewRequest request) {

        interViewService.inactivateInterview(JwtUtil.getAuthenticatedUserId(),
                request.getInterviewIds());

        return ResponseEntity.ok(
                new ApiResponse<>(InterviewMessageCode.INTERVIEW_INACTIVATE_SUCCESS.getMessage(),
                        InterviewMessageCode.INTERVIEW_INACTIVATE_SUCCESS.getCode()));

    }

    @Operation(
            summary = "면접 질문 기록 조회",
            description = " 면접 질문을 언제 생성했는지 보여줍니다.."
    )
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<GetInterviewCountResponse>> getInterviewRecords(
            @Parameter(name = "year", description = "연도입니다", required = true, example = "2025")
            @RequestParam Integer year) {

        return ResponseEntity.ok(new ApiResponse<>(INTERVIEW_RECORD_SUCCESS.getMessage(),
                INTERVIEW_RECORD_SUCCESS.getCode(),
                interViewService.getInterviewRecord(JwtUtil.getAuthenticatedUserId(), year)));
    }

    private InterviewResponseDTO.CreateInterviewResponseDTO waitForResult(String resultKey,
            int timeoutSeconds)
            throws Exception {
        for (int i = 0; i < timeoutSeconds; i++) {
            String resultJson = stringRedisTemplate.opsForValue().get(resultKey);
            if (resultJson != null) {
                return objectMapper.readValue(resultJson,
                        InterviewResponseDTO.CreateInterviewResponseDTO.class);
            }
            Thread.sleep(1000);
        }
        throw new InterviewException.InterviewCreateTimeoutException();
    }
}

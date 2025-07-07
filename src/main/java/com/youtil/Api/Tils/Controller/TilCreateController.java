package com.youtil.Api.Tils.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO.CreateRequestId;
import com.youtil.Api.Tils.Queue.TilQueueProducer;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Exception.TilException.TilException.TilCreateTimeOutException;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "tils", description = "TIL 관련 API")
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
@Slf4j
public class TilCreateController {

    private final TilQueueProducer tilQueueProducer;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    @Qualifier("tilServiceConstants")
    private final AiServiceConstants tilServiceConstants;

    @Operation(
            summary = "TIL 생성",
            description = "커밋 정보에 기반한 AI 내용 생성 및 TIL 저장을 하나의 요청으로 처리합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "TIL 생성 성공",
                    content = @Content(schema = @Schema(implementation = TilResponseDTO.CreateTilResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "해당하는 유저가 존재하지 않습니다."
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
    @PostMapping(
            value = "",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilResponseDTO.CreateRequestId>> createTil(
            @RequestBody TilRequestDTO.CreateWithAiRequest request) {

        log.info("TIL 생성 요청 - 레포지토리: {}, 제목: {}",
                request.getRepositoryId(), request.getTitle());

        try {

            if (request.getRepositoryId() == null) {
                throw new IllegalArgumentException(
                        TilMessageCode.TIL_REPOSITORY_ID_REQUIRED.getMessage());
            }

            if (request.getBranch() == null || request.getBranch().isEmpty()) {
                throw new IllegalArgumentException(TilMessageCode.TIL_BRANCH_REQUIRED.getMessage());
            }

            if (request.getCommits() == null || request.getCommits().isEmpty()) {
                throw new IllegalArgumentException(
                        TilMessageCode.TIL_COMMITS_REQUIRED.getMessage());
            }

            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException(TilMessageCode.TIL_TITLE_REQUIRED.getMessage());
            }

            if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        TilMessageCode.TIL_CATEGORY_REQUIRED.getMessage());
            }

            if (request.getIsShared() == null) {
                throw new IllegalArgumentException(
                        TilMessageCode.TIL_SHARED_STATUS_REQUIRED.getMessage());
            }

            Long userId = JwtUtil.getAuthenticatedUserId();

            String requestId = tilQueueProducer.enqueueTilRequest(userId, request);
            String resultKey = tilServiceConstants.getResultKey() + requestId;

            CreateRequestId response = CreateRequestId.builder()
                    .requestId(requestId).build();

//            TilResponseDTO.CreateTilResponse response = waitForResult(resultKey,
//                    tilServiceConstants.getResendTimeoutSeconds());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ApiResponse<>(TilMessageCode.TIL_CREATED.getMessage(),
                            TilMessageCode.TIL_CREATED.getCode(),
                            response)
            );

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    TilMessageCode.TIL_CREATION_ERROR.getMessage() + ": " + e.getMessage());
        }
    }

    private TilResponseDTO.CreateTilResponse waitForResult(String resultKey, int timeoutSeconds)
            throws Exception {
        for (int i = 0; i < timeoutSeconds; i++) {
            String resultJson = stringRedisTemplate.opsForValue().get(resultKey);
            if (resultJson != null) {
                return objectMapper.readValue(resultJson, TilResponseDTO.CreateTilResponse.class);
            }
            Thread.sleep(1000);
        }
        throw new TilCreateTimeOutException();
    }
}


package com.youtil.Api.Tils.Controller;

import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitDetailService;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Service.TilAiService;
import com.youtil.Api.Tils.Service.TilCreateService;
import com.youtil.Common.ApiResponse;
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
@Tag(name = "tils", description = "TIL 관련 API")
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
@Slf4j
public class TilCreateController {

    private final TilCreateService tilCreateService;
    private final GithubCommitDetailService githubCommitDetailService;
    private final TilAiService tilAiService;

    @Operation(
            summary = "TIL 생성",
            description = "GitHub 커밋 정보를 기반으로 새로운 TIL을 생성합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "TIL 생성 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @PostMapping(
            value = "",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilResponseDTO.CreateTilResponse>> createTil(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "TIL 생성 요청 데이터",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TilRequestDTO.CreateTilRequest.class))
            )
            @RequestBody TilRequestDTO.CreateTilRequest request) {

        log.info("TIL 생성 요청 - 레포지토리: {}, 제목: {}", request.getRepo(), request.getTitle());

        // 토큰에서 인증된 사용자 ID 가져오기
        long userId = JwtUtil.getAuthenticatedUserId();

        // 서비스 호출하여 TIL 생성
        TilResponseDTO.CreateTilResponse tilResponse = tilCreateService.createTil(request, userId);

        // 응답 생성
        ApiResponse<TilResponseDTO.CreateTilResponse> response = ApiResponse.<TilResponseDTO.CreateTilResponse>builder()
                .success(true)
                .code("201")
                .message("성공했습니다.")
                .data(tilResponse)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "AI 기반 TIL 내용 생성",
            description = "GitHub 커밋 정보를 기반으로 AI가 TIL 내용을 생성합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "TIL 내용 생성 성공",
                    content = @Content(schema = @Schema(implementation = TilAiResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @PostMapping(
            value = "/ai/generate",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilAiResponseDTO>> generateTilContent(
            @RequestBody CommitDetailRequestDTO.CommitDetailRequest request) {

        log.info("AI TIL 내용 생성 요청 - 레포지토리: {}, 브랜치: {}",
                request.getRepositoryId(), request.getBranch());

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

            // 1. GitHub에서 선택한 커밋의 상세 정보 조회
            CommitDetailResponseDTO.CommitDetailResponse commitDetail =
                    githubCommitDetailService.getCommitDetails(request, userId);

            // 2. AI API로 TIL 내용 생성 요청
            TilAiResponseDTO aiResponse = tilAiService.generateTilContent(commitDetail);

            // 응답 생성
            ApiResponse<TilAiResponseDTO> response = ApiResponse.<TilAiResponseDTO>builder()
                    .success(true)
                    .code("200")
                    .message("TIL 내용 생성에 성공했습니다.")
                    .data(aiResponse)
                    .build();

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("TIL 내용 생성 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "TIL 내용 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
            summary = "AI 생성 TIL 저장",
            description = "AI가 생성한 TIL 내용을 저장합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "TIL 저장 성공",
                    content = @Content(schema = @Schema(implementation = TilResponseDTO.CreateTilResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @PostMapping(
            value = "/ai/save",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilResponseDTO.CreateTilResponse>> saveTilFromAi(
            @RequestBody TilRequestDTO.CreateAiTilRequest request) {

        log.info("AI 생성 TIL 저장 요청 - 레포지토리: {}, 제목: {}", request.getRepo(), request.getTitle());

        try {
            // 토큰에서 인증된 사용자 ID 가져오기
            long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스 호출하여 TIL 생성
            TilResponseDTO.CreateTilResponse tilResponse = tilCreateService.createTilFromAi(request, userId);

            // 응답 생성
            ApiResponse<TilResponseDTO.CreateTilResponse> response = ApiResponse.<TilResponseDTO.CreateTilResponse>builder()
                    .success(true)
                    .code("201")
                    .message("TIL이 성공적으로 저장되었습니다.")
                    .data(tilResponse)
                    .build();

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("TIL 저장 오류: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "TIL 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
package com.youtil.Api.Tils.Controller;

import com.youtil.Api.Tils.Dto.TilResponseDTO;
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
@Tag(name = "tils", description = "TIL 조회 관련 API")
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
@Slf4j
public class TilReadController {

    private final TilCreateService tilCreateService;

    @Operation(
            summary = "내 TIL 목록 조회",
            description = "현재 로그인한 사용자의 TIL 목록을 페이징하여 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "TIL 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = TilResponseDTO.TilListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilResponseDTO.TilListResponse>> getMyTils(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        log.info("내 TIL 목록 조회 요청 - 페이지: {}, 사이즈: {}", page, size);

        try {
            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스를 통해 TIL 목록 조회
            TilResponseDTO.TilListResponse response = tilCreateService.getUserTils(userId, page, size);

            // 응답 생성
            ApiResponse<TilResponseDTO.TilListResponse> apiResponse = ApiResponse.<TilResponseDTO.TilListResponse>builder()
                    .success(true)
                    .code("200")
                    .message("내 TIL 목록 조회 성공")
                    .data(response)
                    .build();

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("내 TIL 목록 조회 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "TIL 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
            summary = "내 연도별 TIL 목록 조회",
            description = "현재 로그인한 사용자의 특정 연도 TIL 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "연도별 TIL 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = TilResponseDTO.TilYearListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @GetMapping(
            value = "/years/{year}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilResponseDTO.TilYearListResponse>> getMyTilsByYear(
            @PathVariable("year") int year) {

        log.info("내 연도별 TIL 목록 조회 요청 - 연도: {}", year);

        try {
            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스를 통해 연도별 TIL 목록 조회
            TilResponseDTO.TilYearListResponse response = tilCreateService.getTilsByYear(userId, year);

            // 응답 생성
            ApiResponse<TilResponseDTO.TilYearListResponse> apiResponse = ApiResponse.<TilResponseDTO.TilYearListResponse>builder()
                    .success(true)
                    .code("200")
                    .message("내 연도별 TIL 목록 조회 성공")
                    .data(response)
                    .build();

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            log.error("내 연도별 TIL 목록 조회 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "연도별 TIL 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Operation(
            summary = "내 TIL 상세 조회",
            description = "현재 로그인한 사용자의 특정 TIL 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "TIL 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = TilResponseDTO.TilDetailResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "TIL을 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @GetMapping(
            value = "/{tilId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<TilResponseDTO.TilDetailResponse>> getMyTilById(
            @PathVariable("tilId") Long tilId) {

        log.info("내 TIL 상세 조회 요청 - TIL ID: {}", tilId);

        try {
            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스를 통해 TIL 상세 정보 조회
            TilResponseDTO.TilDetailResponse response = tilCreateService.getTilById(tilId, userId);

            // 본인 소유의 TIL인지 확인
            if (!response.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 TIL만 접근할 수 있습니다.");
            }

            // 응답 생성
            ApiResponse<TilResponseDTO.TilDetailResponse> apiResponse = ApiResponse.<TilResponseDTO.TilDetailResponse>builder()
                    .success(true)
                    .code("200")
                    .message("내 TIL 상세 조회 성공")
                    .data(response)
                    .build();

            return ResponseEntity.ok(apiResponse);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                log.warn("TIL을 찾을 수 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            } else if (e.getMessage().contains("접근 권한이 없습니다")) {
                log.warn("TIL 접근 권한 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
            } else if (e.getMessage().contains("삭제된 TIL입니다")) {
                log.warn("삭제된 TIL: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.GONE, e.getMessage());
            } else {
                log.error("TIL 상세 조회 오류: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "TIL 상세 조회 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }
}
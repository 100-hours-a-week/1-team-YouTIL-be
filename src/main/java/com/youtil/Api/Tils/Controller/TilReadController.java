package com.youtil.Api.Tils.Controller;

import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Service.TilCommendService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@Tag(name = "tils", description = "TIL 조회 관련 API")
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
@Slf4j
public class TilReadController {

    private final TilCommendService tilCommendService;

    @Operation(
            summary = "TIL 목록 조회 (전체 또는 날짜별)",
            description = "현재 로그인한 사용자의 TIL 목록을 조회합니다. 날짜를 'yyyy-MM-dd' 형식으로 쿼리 파라미터로 전달하면 해당 날짜의 목록만 조회됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "TIL 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = TilResponseDTO.TilListResponse.class))
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
                    responseCode = "500",
                    description = "서버 오류"
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<TilResponseDTO.TilListResponse>> getMyTilsWithOptionalDate(
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        log.info("TIL 목록 조회 요청 - 날짜: {}, 페이지: {}, 사이즈: {}", dateStr, page, size);

        try {
            Long userId = JwtUtil.getAuthenticatedUserId();
            TilResponseDTO.TilListResponse response;

            if (dateStr != null && !dateStr.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                    response = tilCommendService.getUserTilsByDate(userId, date, page, size);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. 'yyyy-MM-dd' 형식을 사용해주세요.");
                }
            } else {
                response = tilCommendService.getUserTils(userId, page, size);
            }

            ApiResponse<TilResponseDTO.TilListResponse> apiResponse = new ApiResponse<>(
                    TilMessageCode.TIL_LIST_FETCHED.getMessage(),
                    TilMessageCode.TIL_LIST_FETCHED.getCode(),
                    response);

            return ResponseEntity.ok(apiResponse);

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 요청: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("TIL 목록 조회 오류: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "TIL 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
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
            Long userId = JwtUtil.getAuthenticatedUserId();
            TilResponseDTO.TilDetailResponse response = tilCommendService.getTilById(tilId, userId);

            if (!response.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        TilMessageCode.TIL_ACCESS_DENIED.getMessage());
            }

            ApiResponse<TilResponseDTO.TilDetailResponse> apiResponse = new ApiResponse<>(
                    TilMessageCode.TIL_DETAIL_FETCHED.getMessage(),
                    TilMessageCode.TIL_DETAIL_FETCHED.getCode(),
                    response);

            return ResponseEntity.ok(apiResponse);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                log.warn("TIL을 찾을 수 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        TilMessageCode.TIL_NOT_FOUND.getMessage());
            } else if (e.getMessage().contains("접근 권한이 없습니다")) {
                log.warn("TIL 접근 권한 없음: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        TilMessageCode.TIL_ACCESS_DENIED.getMessage());
            } else if (e.getMessage().contains("삭제된 TIL입니다")) {
                log.warn("삭제된 TIL: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.GONE,
                        TilMessageCode.TIL_ALREADY_DELETED.getMessage());
            } else {
                log.error("TIL 상세 조회 오류: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        TilMessageCode.TIL_SERVER_ERROR.getMessage());
            }
        }
    }
}

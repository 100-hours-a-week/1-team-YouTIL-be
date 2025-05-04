package com.youtil.Api.Tils.Controller;

import com.youtil.Api.Tils.Dto.TilRequestDTO;
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

@RestController
@Tag(name = "tils", description = "TIL 관련 API")
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
@Slf4j
public class TilCreateController {

    private final TilCreateService tilCreateService;

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
}
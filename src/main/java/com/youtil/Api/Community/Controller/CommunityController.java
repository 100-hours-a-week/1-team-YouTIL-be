package com.youtil.Api.Community.Controller;

import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Api.Community.Service.CommunityService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.MessageCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@Tag(name = "community", description = "커뮤니티 관련 API")
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
@Slf4j
public class CommunityController {

    private final CommunityService communityService;

    @Operation(
            summary = "최신 TIL 목록 조회",
            description = "커뮤니티에 공개된 최신 TIL 10개를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "최신 TIL 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping(
            value = "/recent-tils",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ApiResponse<List<CommunityResponseDTO.RecentTilItem>> getRecentTils() {
        try {
            CommunityResponseDTO.RecentTilListResponse response = communityService.getRecentTils();

            return new ApiResponse<>(
                    "최신 TIL 목록 조회 성공",
                    "200",
                    response.getTils()
            );

        } catch (Exception e) {
            log.error("최신 TIL 목록 조회 오류: {}", e.getMessage(), e);

            // 실패 시 ApiResponse 객체 생성
            return ApiResponse.<List<CommunityResponseDTO.RecentTilItem>>builder()
                    .success(false)
                    .code("500")
                    .message("서버 내부 오류 입니다.")
                    .responseAt(OffsetDateTime.now())
                    .data(null)
                    .build();
        }
    }
}
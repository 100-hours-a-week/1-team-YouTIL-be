package com.youtil.Api.Community.Controller;

import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Api.Community.Service.CommunityService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.TilMessageCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
                    TilMessageCode.COMMUNITY_RECENT_TILS_FETCHED.getMessage(),
                    TilMessageCode.COMMUNITY_RECENT_TILS_FETCHED.getCode(),
                    response.getTils()
            );

        } catch (Exception e) {
            log.error("최신 TIL 목록 조회 오류: {}", e.getMessage(), e);

            // 실패 시 ApiResponse 객체 생성
            return ApiResponse.<List<CommunityResponseDTO.RecentTilItem>>builder()
                    .success(false)
                    .code(TilMessageCode.TIL_SERVER_ERROR.getCode())
                    .message(TilMessageCode.TIL_SERVER_ERROR.getMessage())
                    .responseAt(OffsetDateTime.now())
                    .data(null)
                    .build();
        }
    }

    @Operation(
            summary = "커뮤니티 목록 조회",
            description = "카테괼별 커뮤니티 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "커뮤니티 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CommunityResponseDTO.CommunityTilListResponse>> getCommunityTils(
            @Parameter(description = "카테고리 (FULLSTACK, AI, CLOUD, ENTIRE)", example = "FULLSTACK")
            @RequestParam(value = "category", required = false) String category,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(value = "offset", required = false, defaultValue = "10") Integer offset) {

        try{
            CommunityRequestDTO.CommunityListRequest request = CommunityRequestDTO.CommunityListRequest.builder()
                    .category(category)
                    .page(page)
                    .offset(offset)
                    .build();

            CommunityResponseDTO.CommunityTilListResponse response = communityService.getCommunityTils(request);

            ApiResponse<CommunityResponseDTO.CommunityTilListResponse> apiResponse = new ApiResponse<>(
                    TilMessageCode.COMMUNITY_TILS_FETCHED.getMessage(),
                    TilMessageCode.COMMUNITY_TILS_FETCHED.getCode(),
                    response
            );
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            ApiResponse<CommunityResponseDTO.CommunityTilListResponse>errorResponse = ApiResponse
                    .<CommunityResponseDTO.CommunityTilListResponse>builder()
                    .success(false)
                    .code(TilMessageCode.TIL_SERVER_ERROR.getCode())
                    .message(TilMessageCode.TIL_SERVER_ERROR.getMessage())
                    .responseAt(OffsetDateTime.now())
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}

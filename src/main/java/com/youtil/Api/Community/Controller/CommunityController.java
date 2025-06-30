package com.youtil.Api.Community.Controller;

import com.youtil.Api.Community.Dto.CommunityRequestDTO.CreateCommentRequest;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.CreateCommentResponse;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.GetCommentsResponse.GetCommentListResponseDTO;
import com.youtil.Api.Community.Service.CommunityService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.CommunityMessageCode;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Exception.CommunityException.CommunityException.CommentContentNotFoundException;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            summary = "TIL 댓글 작성",
            description = "커뮤니티에 공개된 최신 TIL 10개를 조회합니다."
    )
    @PostMapping("/{tilId}/comments")
    public ResponseEntity<ApiResponse<CreateCommentResponse>> insertComment(
            @PathVariable Long tilId,
            @RequestBody CreateCommentRequest request) {
        if (request.getContent() == null) {
            throw new CommentContentNotFoundException();
        }
        return new ResponseEntity<>(
                new ApiResponse<>(CommunityMessageCode.COMMENT_CREATED.getCode(),
                        CommunityMessageCode.COMMENT_CREATED.getMessage(),
                        communityService.createComment(
                                JwtUtil.getAuthenticatedUserId(), tilId, request)),
                HttpStatus.CREATED);
    }

    @GetMapping("/{tilId}/comments")
    public ResponseEntity<ApiResponse<GetCommentListResponseDTO>> getComments(
            @PathVariable Long tilId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return new ResponseEntity<>(
                new ApiResponse<>(CommunityMessageCode.FIND_COMMENTS_SUCCESS.getCode(),
                        CommunityMessageCode.FIND_COMMENTS_SUCCESS.getMessage(),
                        communityService.getGuestbookList(tilId, pageable)), HttpStatus.OK);


    }

}

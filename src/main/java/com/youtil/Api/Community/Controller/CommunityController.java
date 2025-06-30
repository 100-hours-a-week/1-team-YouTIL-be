package com.youtil.Api.Community.Controller;


import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Api.Community.Dto.CommunityRequestDTO.CreateCommentRequest;
import com.youtil.Api.Community.Dto.CommunityRequestDTO.EditCommentRequest;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.CreateCommentResponse;
import com.youtil.Api.Community.Service.CommunityService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.CommunityMessageCode;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Exception.CommunityException.CommunityException.CommentContentNotFoundException;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
            summary = "커뮤니티 목록 조회",
            description = "카테고리별 커뮤니티 목록을 조회합니다."
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

            @Parameter(description = "페이지 번호", example = "0")
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(value = "offset", required = false, defaultValue = "10") Integer offset) {

        try {
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
            ApiResponse<CommunityResponseDTO.CommunityTilListResponse> errorResponse = ApiResponse
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

    @Operation(
            summary = "커뮤니티 게시글 상세 조회",
            description = "특정 커뮤니티 게시글의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "커뮤니티 게시글 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "해당하는 게시글이 존재하지 않습니다.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping(
            value = "/{tilId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<CommunityResponseDTO.CommunityPostDetailResponse>> getCommunityPostDetail(
            @Parameter(description = "TIL ID", example = "1", required = true)
            @PathVariable("tilId") Long tilId) {

        log.info("커뮤니티 게시글 상세 조회 요청 - TIL ID: {}", tilId);

        try {
            CommunityResponseDTO.CommunityPostDetailResponse response =
                    communityService.getCommunityPostDetail(tilId);

            ApiResponse<CommunityResponseDTO.CommunityPostDetailResponse> apiResponse = new ApiResponse<>(
                    TilMessageCode.COMMUNITY_POST_DETAIL_FETCHED.getMessage(),
                    TilMessageCode.COMMUNITY_POST_DETAIL_FETCHED.getCode(),
                    response
            );

            return ResponseEntity.ok(apiResponse);

        } catch (RuntimeException e) {
            log.warn("커뮤니티 게시글 상세 조회 실패 - TIL ID: {}, 오류: {}", tilId, e.getMessage());

            ApiResponse<CommunityResponseDTO.CommunityPostDetailResponse> errorResponse = ApiResponse
                    .<CommunityResponseDTO.CommunityPostDetailResponse>builder()
                    .success(false)
                    .code("400")
                    .message("해당하는 게시글이 존재하지 않습니다.")
                    .responseAt(OffsetDateTime.now())
                    .data(null)
                    .build();

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("커뮤니티 게시글 상세 조회 오류 - TIL ID: {}, 오류: {}", tilId, e.getMessage(), e);

            ApiResponse<CommunityResponseDTO.CommunityPostDetailResponse> errorResponse = ApiResponse
                    .<CommunityResponseDTO.CommunityPostDetailResponse>builder()
                    .success(false)
                    .code(TilMessageCode.TIL_SERVER_ERROR.getCode())
                    .message(TilMessageCode.TIL_SERVER_ERROR.getMessage())
                    .responseAt(OffsetDateTime.now())
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(
            summary = "커뮤니티 좋아요 토글",
            description = "커뮤니티 게시글에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소되고, 좋아요하지 않은 경우 추가됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요가 반영되었습니다.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "해당하는 유저가 존재하지 않습니다.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping(
            value = "/{tilId}/like",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<CommunityResponseDTO.CommunityLikeResponse>> toggleCommunityLike(
            @Parameter(description = "TIL ID", example = "1", required = true)
            @PathVariable("tilId") Long tilId) {

        try {
            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            // 서비스 호출
            CommunityResponseDTO.CommunityLikeResponse response =
                    communityService.toggleCommunityLike(tilId, userId);

            // 성공 응답
            ApiResponse<CommunityResponseDTO.CommunityLikeResponse> apiResponse = new ApiResponse<>(
                    TilMessageCode.COMMUNITY_LIKE_SUCCESS.getMessage(),
                    TilMessageCode.COMMUNITY_LIKE_SUCCESS.getCode(),
                    response
            );

            return ResponseEntity.ok(apiResponse);

        } catch (RuntimeException e) {
            log.warn("커뮤니티 좋아요 토글 실패 - TIL ID: {}, 오류: {}", tilId, e.getMessage());

            if (e.getMessage().contains("해당하는 유저가 존재하지 않습니다")) {
                // 사용자가 존재하지 않는 경우 400 에러
                ApiResponse<CommunityResponseDTO.CommunityLikeResponse> errorResponse = ApiResponse
                        .<CommunityResponseDTO.CommunityLikeResponse>builder()
                        .success(false)
                        .code(TilMessageCode.COMMUNITY_USER_NOT_FOUND.getCode())
                        .message(TilMessageCode.COMMUNITY_USER_NOT_FOUND.getMessage())
                        .responseAt(OffsetDateTime.now())
                        .data(null)
                        .build();

                return ResponseEntity.badRequest().body(errorResponse);
            } else {
                // 게시글이 존재하지 않는 경우 400 에러
                ApiResponse<CommunityResponseDTO.CommunityLikeResponse> errorResponse = ApiResponse
                        .<CommunityResponseDTO.CommunityLikeResponse>builder()
                        .success(false)
                        .code(TilMessageCode.COMMUNITY_POST_NOT_FOUND.getCode())
                        .message(TilMessageCode.COMMUNITY_POST_NOT_FOUND.getMessage())
                        .responseAt(OffsetDateTime.now())
                        .data(null)
                        .build();

                return ResponseEntity.badRequest().body(errorResponse);
            }

        } catch (Exception e) {
            // 서버 내부 오류
            ApiResponse<CommunityResponseDTO.CommunityLikeResponse> errorResponse = ApiResponse
                    .<CommunityResponseDTO.CommunityLikeResponse>builder()
                    .success(false)
                    .code(TilMessageCode.TIL_SERVER_ERROR.getCode())
                    .message(TilMessageCode.TIL_SERVER_ERROR.getMessage())
                    .responseAt(OffsetDateTime.now())
                    .data(null)
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @Operation(
            summary = "TIL 댓글 작성",
            description = "TIL에 댓글을 작성합니다."
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

    @Operation(
            summary = "TIL 댓글 목록 조회",
            description = "TIL의 댓글 목록을 페이징하여 조회합니다."
    )
    @GetMapping("/{tilId}/comments")
    public ResponseEntity<ApiResponse<CommunityResponseDTO.GetCommentListResponseDTO>> getComments(
            @PathVariable Long tilId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        return new ResponseEntity<>(
                new ApiResponse<>(CommunityMessageCode.FIND_COMMENTS_SUCCESS.getCode(),
                        CommunityMessageCode.FIND_COMMENTS_SUCCESS.getMessage(),
                        communityService.getCommentsList(tilId, pageable)), HttpStatus.OK);
    }

    @Operation(
            summary = "TIL 댓글 수정",
            description = "TIL의 댓글을 수정합니다."
    )
    @PutMapping("/{tilId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<String>> editComment(
            @PathVariable Long tilId,
            @PathVariable Long commentId,
            @RequestBody EditCommentRequest request
    ) {
        if (request.getContent() == null) {
            throw new CommentContentNotFoundException();
        }
        communityService.editComment(tilId, commentId, JwtUtil.getAuthenticatedUserId(), request);
        return ResponseEntity.ok(
                new ApiResponse<>(CommunityMessageCode.COMMENT_EDIT_SUCCESS.getCode(),
                        CommunityMessageCode.COMMENT_EDIT_SUCCESS.getMessage()));
    }

    @Operation(
            summary = "TIL 댓글 삭제",
            description = "TIL의 댓글을 삭제합니다."
    )
    @DeleteMapping("/{tilId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable Long tilId,
            @PathVariable Long commentId
    ) {
        communityService.deleteComment(tilId, commentId, JwtUtil.getAuthenticatedUserId());
        return ResponseEntity.ok(
                new ApiResponse<>(CommunityMessageCode.COMMENT_INACTIVATE_SUCCESS.getCode(),
                        CommunityMessageCode.COMMENT_INACTIVATE_SUCCESS.getMessage())
        );
    }
}

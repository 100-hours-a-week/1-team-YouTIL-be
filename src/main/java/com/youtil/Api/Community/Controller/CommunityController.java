package com.youtil.Api.Community.Controller;

import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Api.Community.Dto.CommunityRequestDTO.CreateCommentRequest;
import com.youtil.Api.Community.Dto.CommunityRequestDTO.EditCommentRequest;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO.CreateCommentResponse;
import com.youtil.Api.Community.Service.CommunityService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.DuplicatePrevention.DuplicatePreventionManager;
import com.youtil.Common.Enums.CommunityMessageCode;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Exception.CommunityException.CommunityException.CommentContentNotFoundException;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@Tag(name = "community", description = "커뮤니티 관련 API")
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
@Slf4j
public class CommunityController {

    private final CommunityService communityService;
    private final DuplicatePreventionManager duplicatePreventionManager;

    @Operation(
            summary = "최신 TIL 목록 조회",
            description = "커뮤니티에 공개된 최신 TIL 10개를 조회합니다."
    )
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
    @GetMapping(
            value = "/{tilId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<CommunityResponseDTO.CommunityPostDetailResponse>> getTilDetail(
            @Parameter(description = "TIL ID", example = "1", required = true)
            @PathVariable("tilId") Long tilId) {

        Long userId = JwtUtil.getAuthenticatedUserId();

        log.info("TIL 상세 조회 요청 - TIL ID: {}", tilId);

        try {
            CommunityResponseDTO.CommunityPostDetailResponse response = communityService.getTilDetail(tilId,userId);
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
            summary = "TIL 좋아요 토글",
            description = "TIL에 좋아요를 추가하거나 취소합니다. 이미 좋아요한 경우 취소되고, 좋아요하지 않은 경우 추가됩니다."
    )
    @PostMapping(
            value = "/{tilId}/like",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<CommunityResponseDTO.CommunityLikeResponse>> toggleTilLike(
            @Parameter(description = "TIL ID", example = "1", required = true)
            @PathVariable("tilId") Long tilId) {

        try {
            // 인증된 사용자 ID 가져오기
            Long userId = JwtUtil.getAuthenticatedUserId();

            if (!duplicatePreventionManager.preventTilRecommendDuplicate(userId, tilId, tilId)) {
                ApiResponse<CommunityResponseDTO.CommunityLikeResponse> errorResponse = ApiResponse
                        .<CommunityResponseDTO.CommunityLikeResponse>builder()
                        .success(false)
                        .code("429")
                        .message("너무 빠른 추천 요청입니다. 잠시 후 다시 시도해주세요.")
                        .responseAt(OffsetDateTime.now())
                        .data(null)
                        .build();
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }

            CommunityResponseDTO.CommunityLikeResponse response = communityService.toggleTilLike(tilId, userId);

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
        Long userId = JwtUtil.getAuthenticatedUserId();
        if (!duplicatePreventionManager.preventCommentDuplicate(userId, tilId, request.getContent(), request)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    new ApiResponse<>("같은 댓글을 너무 빠르게 작성했습니다. 잠시 후 다시 시도해주세요.", "429", null));
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

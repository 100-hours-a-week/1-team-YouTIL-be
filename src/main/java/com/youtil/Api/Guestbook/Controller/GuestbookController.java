package com.youtil.Api.Guestbook.Controller;

import com.youtil.Api.Guestbook.Dto.GuestbookRequestDTO;
import com.youtil.Api.Guestbook.Dto.GuestbookResponseDTO;
import com.youtil.Api.Guestbook.Service.GuestbookService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.GuestbookMessageCode;
import com.youtil.Common.Enums.GuestbookStatus;
import com.youtil.Exception.GuestbookException.GuestbookException;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "guestbook", description = "방명록 관련 API")
@RequestMapping("/api/v1/users/{userId}/guestbooks")
@RequiredArgsConstructor
public class GuestbookController {

    private final GuestbookService guestbookService;

    @Operation(summary = "방명록 작성", description = "특정 유저의 방명록에 글을 작성하는 API")
    @PostMapping("")
    public ResponseEntity<ApiResponse<GuestbookResponseDTO.CreateGuestbookResponseDTO>> createGuestbook(
            @Parameter(name = "userId", description = "방명록 주인의 유저 ID", required = true)
            @PathVariable Long userId,
            @RequestBody GuestbookRequestDTO.CreateGuestbookRequestDTO request) {

        // 요청 데이터 유효성 검증
        validateCreateRequest(request);

        Long guestId = JwtUtil.getAuthenticatedUserId();
        GuestbookResponseDTO.CreateGuestbookResponseDTO response =
                guestbookService.createGuestbook(userId, guestId, request);

        return ResponseEntity.status(201).body(
                new ApiResponse<>(GuestbookMessageCode.CREATE_GUESTBOOK_SUCCESS.getMessage(), "201", response));
    }

    @Operation(summary = "방명록 리스트 조회", description = "특정 유저의 방명록 리스트를 조회하는 API")
    @GetMapping("")
    public ResponseEntity<ApiResponse<GuestbookResponseDTO.GetGuestbookListResponseDTO>> getGuestbookList(
            @Parameter(name = "userId", description = "방명록 주인의 유저 ID", required = true)
            @PathVariable Long userId,
            @Parameter(name = "page", description = "페이지 번호", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(name = "offset", description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int offset) {

        // 페이징 파라미터 유효성 검증
        validatePagingParameters(page, offset);

        Pageable pageable = PageRequest.of(page, offset);
        GuestbookResponseDTO.GetGuestbookListResponseDTO response =
                guestbookService.getGuestbookList(userId, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(GuestbookMessageCode.GET_GUESTBOOK_LIST_SUCCESS.getMessage(), "200", response));
    }

    @Operation(summary = "방명록 수정", description = "자신이 작성한 방명록을 수정하는 API")
    @PutMapping("/{guestbookId}")
    public ResponseEntity<ApiResponse<String>> updateGuestbook(
            @Parameter(name = "userId", description = "방명록 주인의 유저 ID", required = true)
            @PathVariable Long userId,
            @Parameter(name = "guestbookId", description = "수정할 방명록 ID", required = true)
            @PathVariable Long guestbookId,
            @RequestBody GuestbookRequestDTO.UpdateGuestbookRequestDTO request) {

        // 요청 데이터 유효성 검증
        validateUpdateRequest(request);

        Long guestId = JwtUtil.getAuthenticatedUserId();
        guestbookService.updateGuestbook(guestbookId, guestId, request);

        return ResponseEntity.ok(
                new ApiResponse<>(GuestbookMessageCode.UPDATE_GUESTBOOK_SUCCESS.getMessage(), "200"));
    }

    @Operation(summary = "방명록 삭제", description = "자신이 작성한 방명록을 삭제하는 API")
    @DeleteMapping("/{guestbookId}")
    public ResponseEntity<ApiResponse<String>> deleteGuestbook(
            @Parameter(name = "userId", description = "방명록 주인의 유저 ID", required = true)
            @PathVariable Long userId,
            @Parameter(name = "guestbookId", description = "삭제할 방명록 ID", required = true)
            @PathVariable Long guestbookId) {

        Long guestId = JwtUtil.getAuthenticatedUserId();
        guestbookService.deleteGuestbook(guestbookId, guestId);

        return ResponseEntity.ok(
                new ApiResponse<>(GuestbookMessageCode.DELETE_GUESTBOOK_SUCCESS.getMessage(), "200"));
    }

    /**
     * 방명록 작성 요청 유효성 검증
     */
    private void validateCreateRequest(GuestbookRequestDTO.CreateGuestbookRequestDTO request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new GuestbookException.GuestbookContentEmptyException();
        }

        if (request.getContent().length() > GuestbookStatus.MAX_CONTENT_LENGTH) {
            throw new GuestbookException.GuestbookContentTooLongException();
        }
    }

    /**
     * 방명록 수정 요청 유효성 검증
     */
    private void validateUpdateRequest(GuestbookRequestDTO.UpdateGuestbookRequestDTO request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new GuestbookException.GuestbookContentEmptyException();
        }

        if (request.getContent().length() > GuestbookStatus.MAX_CONTENT_LENGTH) {
            throw new GuestbookException.GuestbookContentTooLongException();
        }
    }

    /**
     * 페이징 파라미터 유효성 검증
     */
    private void validatePagingParameters(int page, int offset) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (offset <= 0 || offset > GuestbookStatus.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("페이지 크기는 1 이상 %d 이하여야 합니다.", GuestbookStatus.MAX_PAGE_SIZE));
        }
    }
}

package com.youtil.Api.Guestbook.Controller;

import com.youtil.Api.Guestbook.dto.GuestbookRequestDTO;
import com.youtil.Api.Guestbook.dto.GuestbookResponseDTO;
import com.youtil.Api.Guestbook.Service.GuestbookService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.DuplicatePrevention.PreventDuplicate;
import com.youtil.Common.Enums.GuestbookMessageCode;
import com.youtil.Exception.GuestbookException.GuestbookException;
import com.youtil.Util.GuestbookValidationUtils;
import com.youtil.Util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    @PreventDuplicate(action = "guestbook_create", dataFields = {"userId"})
    @PostMapping("")
    public ResponseEntity<ApiResponse<GuestbookResponseDTO.CreateGuestbookResponseDTO>> createGuestbook(
            @Parameter(name = "userId", description = "방명록 주인의 유저 ID", required = true)
            @PathVariable Long userId,
            @RequestBody GuestbookRequestDTO.CreateGuestbookRequestDTO request) {

        try {
            // 방명록 주인 유저ID 검증
            GuestbookValidationUtils.validateUserId(userId);
            // 방명록 작성 요청 데이터 검증
            GuestbookValidationUtils.validateCreateRequest(request.getContent(), request.getTopGuestbookId());

            // 방명록 작성자 유저 ID 검증
            Long guestId = JwtUtil.getAuthenticatedUserId();
            GuestbookValidationUtils.validateUserId(guestId);

            GuestbookResponseDTO.CreateGuestbookResponseDTO response =
                    guestbookService.createGuestbook(userId, guestId, request);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ApiResponse<>(GuestbookMessageCode.CREATE_GUESTBOOK_SUCCESS.getMessage(),
                            GuestbookMessageCode.CREATE_GUESTBOOK_SUCCESS.getCode(), response));
        } catch (Exception e) {
            log.error("방명록 작성 중 오류 발생: {}", e.getMessage(), e);

            // 예외를 GuestbookMessageCode로 변환
            GuestbookMessageCode errorCode = determineErrorMessageCode(e);
            HttpStatus status = determineHttpStatus(e);

            return ResponseEntity.status(status).body(
                    new ApiResponse<>(errorCode.getMessage(), String.valueOf(status.value()), null));
        }
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

        try {
            // 통합된 유효성 검증 사용
            GuestbookValidationUtils.validateUserId(userId);
            GuestbookValidationUtils.validatePagingParameters(page, offset);

            Pageable pageable = PageRequest.of(page, offset);
            GuestbookResponseDTO.GetGuestbookListResponseDTO response =
                    guestbookService.getGuestbookList(userId, pageable);

            return ResponseEntity.ok(
                    new ApiResponse<>(GuestbookMessageCode.GET_GUESTBOOK_LIST_SUCCESS.getMessage(),
                            GuestbookMessageCode.GET_GUESTBOOK_LIST_SUCCESS.getCode(), response));
        } catch (Exception e) {
            log.error("방명록 리스트 조회 중 오류 발생: {}", e.getMessage(), e);

            GuestbookMessageCode errorCode = determineErrorMessageCode(e);
            HttpStatus status = determineHttpStatus(e);

            return ResponseEntity.status(status).body(
                    new ApiResponse<>(errorCode.getMessage(), String.valueOf(status.value()), null));
        }
    }

    @Operation(summary = "방명록 수정", description = "자신이 작성한 방명록을 수정하는 API")
    @PreventDuplicate(action = "guestbook_update", dataFields = {"userId", "guestbookId"})
    @PutMapping("/{guestbookId}")
    public ResponseEntity<ApiResponse<Object>> updateGuestbook(
            @Parameter(name = "userId", description = "방명록 주인의 유저 ID", required = true)
            @PathVariable Long userId,
            @Parameter(name = "guestbookId", description = "수정할 방명록 ID", required = true)
            @PathVariable Long guestbookId,
            @RequestBody GuestbookRequestDTO.UpdateGuestbookRequestDTO request) {

        try {
            // ValidationUtils 활용한 통합 검증
            Long guestId = JwtUtil.getAuthenticatedUserId();
            GuestbookValidationUtils.validateUpdateRequest(request.getContent(), guestbookId, guestId);
            GuestbookValidationUtils.validateUserId(userId);

            // 실제 사용자 존재 여부 검증은 Service에서 처리
            guestbookService.updateGuestbook(userId, guestbookId, guestId, request);

            // 성공 시 빈 Map 반환 (Jackson이 직렬화 가능)
            return ResponseEntity.ok(
                    new ApiResponse<>(GuestbookMessageCode.UPDATE_GUESTBOOK_SUCCESS.getMessage(),
                            "200", java.util.Collections.emptyMap()));
        } catch (Exception e) {
            log.error("방명록 수정 중 오류 발생: {}", e.getMessage(), e);

            GuestbookMessageCode errorCode = determineErrorMessageCode(e);
            HttpStatus status = determineHttpStatus(e);

            return ResponseEntity.status(status).body(
                    new ApiResponse<>(errorCode.getMessage(), String.valueOf(status.value()), null));
        }
    }

    @Operation(summary = "방명록 삭제", description = "자신이 작성한 방명록을 삭제하는 API")
    @PreventDuplicate(action = "guestbook_delete", dataFields = {"userId", "guestbookId"})
    @DeleteMapping("/{guestbookId}")
    public ResponseEntity<ApiResponse<Object>> deleteGuestbook(
            @Parameter(name = "userId", description = "방명록 주인의 유저 ID", required = true)
            @PathVariable Long userId,
            @Parameter(name = "guestbookId", description = "삭제할 방명록 ID", required = true)
            @PathVariable Long guestbookId) {

        try {
            // ValidationUtils 활용한 통합 검증
            Long guestId = JwtUtil.getAuthenticatedUserId();
            GuestbookValidationUtils.validateDeleteRequest(guestbookId, guestId);
            GuestbookValidationUtils.validateUserId(userId);

            // 올바른 파라미터 순서로 호출 (ownerId, guestbookId, guestId)
            guestbookService.deleteGuestbook(userId, guestbookId, guestId);

            return ResponseEntity.ok(
                    new ApiResponse<>(GuestbookMessageCode.DELETE_GUESTBOOK_SUCCESS.getMessage(),
                            "200", java.util.Collections.emptyMap()));
        } catch (Exception e) {
            log.error("방명록 삭제 중 오류 발생: {}", e.getMessage(), e);

            GuestbookMessageCode errorCode = determineErrorMessageCode(e);
            HttpStatus status = determineHttpStatus(e);

            return ResponseEntity.status(status).body(
                    new ApiResponse<>(errorCode.getMessage(), String.valueOf(status.value()), null));
        }
    }

    /**
     * 예외를 GuestbookMessageCode로 변환
     */
    private GuestbookMessageCode determineErrorMessageCode(Exception e) {
        log.debug("예외 타입: {}, 메시지: {}", e.getClass().getSimpleName(), e.getMessage());

        // UserNotFoundException 직접 처리
        if (e.getClass().getSimpleName().contains("UserNotFoundException") ||
                e instanceof com.youtil.Exception.UserException.UserException.UserNotFoundException) {
            return GuestbookMessageCode.USER_NOT_FOUND;
        }

        // EntityValidator에서 발생하는 예외 처리
        if (e.getClass().getSimpleName().contains("UserNotFound") ||
                (e.getMessage() != null && e.getMessage().contains("User not found"))) {
            return GuestbookMessageCode.USER_NOT_FOUND;
        }

        // 사용자 관련 예외 처리 (메시지 기반)
        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();

            if (message.contains("유저를 찾을 수 없거나") || message.contains("탈퇴한 계정")) {
                return GuestbookMessageCode.USER_NOT_FOUND;
            }

            if (message.contains("사용자") || message.contains("유저") || message.contains("user")) {
                if (message.contains("존재하지 않") || message.contains("찾을 수 없") ||
                        message.contains("not found") || message.contains("does not exist")) {
                    return GuestbookMessageCode.USER_NOT_FOUND;
                }
                if (message.contains("유효하지 않은") || message.contains("invalid")) {
                    return GuestbookMessageCode.INVALID_USER_ID;
                }
            }

            if (message.contains("페이지") || message.contains("page") || message.contains("paging")) {
                return GuestbookMessageCode.INVALID_PAGING_PARAMETERS;
            }
        }

        // GuestbookException 타입별 처리
        if (e instanceof GuestbookException.GuestbookContentEmptyException) {
            return GuestbookMessageCode.GUESTBOOK_CONTENT_EMPTY;
        }

        if (e instanceof GuestbookException.GuestbookContentTooLongException) {
            return GuestbookMessageCode.GUESTBOOK_CONTENT_TOO_LONG;
        }

        if (e instanceof GuestbookException.InvalidGuestbookContentException) {
            return GuestbookMessageCode.INVALID_GUESTBOOK_CONTENT;
        }

        if (e instanceof GuestbookException.GuestbookReplyDepthExceededException) {
            return GuestbookMessageCode.GUESTBOOK_REPLY_DEPTH_EXCEEDED;
        }

        if (e instanceof GuestbookException.GuestbookNotFoundException) {
            return GuestbookMessageCode.GUESTBOOK_NOT_FOUND;
        }

        if (e instanceof GuestbookException.InvalidParentGuestbookException) {
            return GuestbookMessageCode.INVALID_PARENT_GUESTBOOK;
        }

        if (e instanceof GuestbookException.InvalidGuestbookAccessException) {
            return GuestbookMessageCode.INVALID_GUESTBOOK_ACCESS;
        }

        if (e instanceof GuestbookException.CannotReplyToDeletedGuestbookException) {
            return GuestbookMessageCode.CANNOT_REPLY_TO_DELETED_GUESTBOOK;
        }

        // 기타 GuestbookException인 경우
        if (e instanceof GuestbookException) {
            GuestbookException guestbookException = (GuestbookException) e;
            if (guestbookException.getMessageCode() != null) {
                return guestbookException.getMessageCode();
            }
        }

        // 기타 예외의 경우 서버 내부 오류
        return GuestbookMessageCode.INTERNAL_SERVER_ERROR;
    }

    /**
     * 예외에 따른 HTTP 상태 코드 결정
     */
    private HttpStatus determineHttpStatus(Exception e) {
        log.debug("HTTP 상태 코드 결정 - 예외: {}", e.getClass().getSimpleName());

        // UserNotFoundException 직접 처리
        if (e.getClass().getSimpleName().contains("UserNotFoundException") ||
                e instanceof com.youtil.Exception.UserException.UserException.UserNotFoundException) {
            return HttpStatus.BAD_REQUEST; // 400
        }

        // EntityValidator 관련 예외
        if (e.getClass().getSimpleName().contains("UserNotFound")) {
            return HttpStatus.BAD_REQUEST; // 400
        }

        // 사용자 관련 예외
        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();
            if (message.contains("유저를 찾을 수 없거나") || message.contains("탈퇴한 계정")) {
                return HttpStatus.BAD_REQUEST; // 400
            }

            if ((message.contains("사용자") || message.contains("유저") || message.contains("user")) &&
                    (message.contains("존재하지 않") || message.contains("찾을 수 없") ||
                            message.contains("not found") || message.contains("does not exist"))) {
                return HttpStatus.BAD_REQUEST; // 400
            }
        }

        if (e instanceof GuestbookException.GuestbookContentEmptyException ||
                e instanceof GuestbookException.GuestbookContentTooLongException ||
                e instanceof GuestbookException.InvalidGuestbookContentException ||
                e instanceof GuestbookException.GuestbookReplyDepthExceededException ||
                e instanceof GuestbookException.CannotReplyToDeletedGuestbookException) {
            return HttpStatus.BAD_REQUEST; // 400
        }

        if (e instanceof GuestbookException.GuestbookNotFoundException ||
                e instanceof GuestbookException.InvalidParentGuestbookException) {
            return HttpStatus.NOT_FOUND; // 404
        }

        if (e instanceof GuestbookException.InvalidGuestbookAccessException) {
            return HttpStatus.FORBIDDEN; // 403
        }

        // 기타 예외의 경우 500 Internal Server Error
        return HttpStatus.INTERNAL_SERVER_ERROR; // 500
    }
}

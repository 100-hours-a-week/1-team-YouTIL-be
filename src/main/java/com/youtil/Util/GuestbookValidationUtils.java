package com.youtil.Util;

import com.youtil.Common.Enums.GuestbookStatus;
import com.youtil.Exception.GuestbookException.GuestbookException;

public final class GuestbookValidationUtils {

    /**
     * 방명록 내용 유효성 검증
     * @param content 검증할 내용
     * @throws GuestbookException 유효하지 않은 경우
     */
    public static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new GuestbookException.GuestbookContentEmptyException();
        }

        if (content.length() > GuestbookStatus.MAX_CONTENT_LENGTH) {
            throw new GuestbookException.GuestbookContentTooLongException();
        }
    }

    /**
     * 페이징 파라미터 유효성 검증
     * @param page 페이지 번호
     * @param offset 페이지 크기
     * @throws GuestbookException 유효하지 않은 경우 (일관성을 위해 변경)
     */
    public static void validatePagingParameters(int page, int offset) {
        if (page < 0) {
            throw new GuestbookException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (offset <= 0) {
            throw new GuestbookException("페이지 크기는 1 이상이어야 합니다.");
        }

        if (offset > GuestbookStatus.MAX_PAGE_SIZE) {
            throw new GuestbookException(
                    String.format("페이지 크기는 %d 이하여야 합니다.", GuestbookStatus.MAX_PAGE_SIZE));
        }
    }

    /**
     * 사용자 ID 유효성 검증
     * @param userId 사용자 ID
     * @throws GuestbookException 유효하지 않은 경우 (일관성을 위해 변경)
     */
    public static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new GuestbookException("유효하지 않은 사용자 ID입니다.");
        }
    }

    /**
     * 방명록 ID 유효성 검증
     * @param guestbookId 방명록 ID
     * @throws GuestbookException 유효하지 않은 경우 (일관성을 위해 변경)
     */
    public static void validateGuestbookId(Long guestbookId) {
        if (guestbookId == null || guestbookId <= 0) {
            throw new GuestbookException("유효하지 않은 방명록 ID입니다.");
        }
    }

    /**
     * 답글 깊이 검증
     * @param depth 현재 깊이
     * @throws GuestbookException 최대 깊이 초과 시
     */
    public static void validateReplyDepth(int depth) {
        if (depth > GuestbookStatus.MAX_REPLY_DEPTH) {
            throw new GuestbookException.GuestbookReplyDepthExceededException();
        }
    }

    /**
     * 방명록 생성 요청 전체 유효성 검증
     * @param content 방명록 내용
     * @param topGuestbookId 상위 방명록 ID (nullable)
     * @throws GuestbookException 유효하지 않은 경우
     */
    public static void validateCreateRequest(String content, Long topGuestbookId) {
        validateContent(content);
        if (topGuestbookId != null) {
            validateGuestbookId(topGuestbookId);
        }
    }

    /**
     * 방명록 수정 요청 전체 유효성 검증
     * @param content 수정할 내용
     * @param guestbookId 방명록 ID
     * @param guestId 작성자 ID
     * @throws GuestbookException 유효하지 않은 경우
     */
    public static void validateUpdateRequest(String content, Long guestbookId, Long guestId) {
        validateContent(content);
        validateGuestbookId(guestbookId);
        validateUserId(guestId);
    }

    /**
     * 방명록 삭제 요청 전체 유효성 검증
     * @param guestbookId 방명록 ID
     * @param guestId 작성자 ID
     * @throws GuestbookException 유효하지 않은 경우
     */
    public static void validateDeleteRequest(Long guestbookId, Long guestId) {
        validateGuestbookId(guestbookId);
        validateUserId(guestId);
    }

    // 생성자를 private으로 선언하여 인스턴스 생성 방지
    private GuestbookValidationUtils() {
        throw new AssertionError("GuestbookValidationUtils 클래스는 인스턴스화할 수 없습니다.");
    }
}

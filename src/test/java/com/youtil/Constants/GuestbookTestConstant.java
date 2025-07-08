package com.youtil.Constants;

import java.time.OffsetDateTime;

/**
 * 방명록 테스트용 상수 클래스
 * 테스트 데이터의 일관성과 재사용성을 위한 상수 정의
 */
public class GuestbookTestConstant {

    // === 사용자 ID 관련 ===
    public static final Long VALID_OWNER_ID = 1L;
    public static final Long VALID_GUEST_ID = 2L;
    public static final Long ANOTHER_USER_ID = 3L;
    public static final Long INVALID_USER_ID = 999L;

    // === 방명록 ID 관련 ===
    public static final Long VALID_GUESTBOOK_ID = 100L;
    public static final Long VALID_PARENT_GUESTBOOK_ID = 101L;
    public static final Long INVALID_GUESTBOOK_ID = 999L;

    // === 방명록 내용 관련 ===
    public static final String VALID_CONTENT = "방명록 내용입니다.";
    public static final String LONG_CONTENT = "가".repeat(51);
    public static final String EMPTY_CONTENT = "";
    public static final String NULL_CONTENT = null;

    // === 사용자 정보 관련 ===
    public static final String GUEST_NICKNAME = "게스트사용자";
    public static final String OWNER_NICKNAME = "방명록주인";
    public static final String GUEST_PROFILE_URL = "https://example.com/profile.jpg";

    // === 페이징 관련 ===
    public static final int VALID_PAGE = 0;
    public static final int VALID_SIZE = 20;

    // === 날짜/시간 관련 ===
    public static final OffsetDateTime NOW = OffsetDateTime.now();
    public static final OffsetDateTime CREATED_AT = NOW.minusDays(1);
    public static final OffsetDateTime UPDATED_AT = NOW.minusHours(1);
    public static final OffsetDateTime DELETED_AT = NOW.minusMinutes(10);

    // === 카운트 관련 ===
    public static final long ZERO_COUNT = 0L;
    public static final long ONE_COUNT = 1L;

    // === 예외 메시지 관련 ===
    public static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    public static final String DELETED_COMMENT_MESSAGE = "삭제된 댓글입니다.";

    // === 테스트 시나리오별 데이터 ===
    public static final class CreateScenario {
        public static final String SUCCESS_CONTENT = "새로운 방명록입니다.";
        public static final String REPLY_CONTENT = "답글입니다.";
    }

    public static final class UpdateScenario {
        public static final String NEW_CONTENT = "새로운 내용";
    }

    public static final class DeleteScenario {
        public static final String BEFORE_DELETE_CONTENT = "삭제 전 내용";
        public static final Long PARENT_WITH_REPLIES_ID = 200L;
    }

    private GuestbookTestConstant() {
        throw new AssertionError("Constants class should not be instantiated");
    }
}

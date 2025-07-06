package com.youtil.constant;

import com.youtil.Common.Enums.Status;

/**
 * 커뮤니티 테스트 상수
 */
public class CommunityTestConstant {

    // 사용자 관련 상수
    public static final Long VALID_USER_ID = 1L;
    public static final Long INVALID_USER_ID = 999L;
    public static final String USER_NICKNAME = "testuser";
    public static final String USER_EMAIL = "test@example.com";
    public static final String USER_PROFILE_IMAGE = "profile.jpg";

    // TIL 관련 상수
    public static final Long VALID_TIL_ID = 1L;
    public static final Long INVALID_TIL_ID = 999L;
    public static final String TIL_TITLE = "테스트 TIL 제목";
    public static final String TIL_CONTENT = "테스트 TIL 내용입니다.";
    public static final String TIL_CATEGORY = "FULLSTACK";
    public static final java.util.List<String> TIL_TAG_LIST = java.util.Arrays.asList("Spring", "Java", "Test");
    public static final java.util.List<String> TIL_TAG_LIST_1 = java.util.Arrays.asList("Java", "Spring");
    public static final java.util.List<String> TIL_TAG_LIST_2 = java.util.Arrays.asList("React", "JavaScript");
    public static final Boolean TIL_IS_DISPLAY_TRUE = true;
    public static final Boolean TIL_IS_DISPLAY_FALSE = false;
    public static final Status TIL_STATUS_ACTIVE = Status.active;
    public static final Status TIL_STATUS_DEACTIVE = Status.deactive;
    public static final Integer TIL_RECOMMEND_COUNT = 5;
    public static final Integer TIL_VISITED_COUNT = 10;
    public static final Integer TIL_COMMENTS_COUNT = 3;

    // 페이징 관련 상수
    public static final Integer DEFAULT_PAGE = 0;
    public static final Integer DEFAULT_SIZE = 10;

    // 카테고리 관련 상수
    public static final String CATEGORY_FULLSTACK = "FULLSTACK";
    public static final String CATEGORY_ENTIRE = "ENTIRE";

    // Redis 관련 상수
    public static final String REDIS_TIL_LIKE_COUNT_KEY = "til:1:like_count";
    public static final String REDIS_TIL_VISIT_COUNT_KEY = "til:1:visit_count";
    public static final String REDIS_CHANGED_TILS_KEY = "changed:tils";

    // 좋아요 관련 상수
    public static final Long VALID_RECOMMEND_ID = 1L;

    // 에러 메시지 상수
    public static final String ERROR_USER_NOT_FOUND = "해당하는 유저가 존재하지 않습니다.";
    public static final String ERROR_TIL_NOT_FOUND = "해당하는 게시글이 존재하지 않습니다.";
}

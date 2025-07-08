package com.youtil.Constants;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class MockTilConstants {

    public static final long MOCK_TIL_ID = 1L;
    public static final String MOCK_TITLE = "title";
    public static final String MOCK_CONTENT = "content";
    public static final List<String> MOCK_TAGS = List.of("tag1", "tag2");
    public static final String MOCK_CATEGORY = "FULLSTACK";
    public static final int INITIAL_COMMENTS_COUNT = 0;
    public static final int INITIAL_VISITED_COUNT = 0;
    public static final boolean IS_DISPLAYED = true;
    public static final int INITIAL_RECOMMEND_COUNT = 0;

    // 추가 테스트 상수들
    public static final Long MOCK_TIL_ID_2 = 2L;
    public static final long INVALID_TIL_ID = 999L;
    public static final String MOCK_CATEGORY_BACKEND = "BACKEND";
    public static final List<String> MOCK_TAGS_SPRING = Arrays.asList("Spring", "Java");
    public static final List<String> MOCK_TAGS_REACT = Arrays.asList("React", "JavaScript");

    // 페이징 관련
    public static final int TIL_DEFAULT_PAGE = 0;
    public static final int TIL_DEFAULT_SIZE = 10;

    // AI 관련
    public static final String AI_RESPONSE_CONTENT = "# 로그인 기능 구현\n\n로그인 기능을 구현했습니다.";
    public static final String AI_HEALTH_OK = "OK";
    public static final List<String> AI_KEYWORDS = Arrays.asList("로그인", "인증", "스프링");

    // 날짜
    public static final LocalDate TEST_DATE = LocalDate.of(2024, 1, 15);

    // 에러 메시지
    public static final String TIL_NOT_FOUND_MESSAGE = "TIL을 찾을 수 없습니다.";
    public static final String TIL_ALREADY_DELETED_MESSAGE = "이미 삭제된 TIL입니다.";
    public static final String TIL_ACCESS_DENIED_MESSAGE = "본인의 TIL만 접근할 수 있습니다.";

    private MockTilConstants() {
    }
}
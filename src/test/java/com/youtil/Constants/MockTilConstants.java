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


    // 테스트 사용자 관련 상수
    public static final long TEST_USER_ID = 1L;
    public static final long OTHER_USER_ID = 2L;
    public static final long INVALID_USER_ID = 999L;
    public static final long INVALID_TIL_ID = 999L;
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_PROFILE_URL = "https://example.com/profile.jpg";

    // 테스트 TIL 관련 상수
    public static final String TEST_TIL_TITLE = "테스트 TIL";
    public static final String TEST_TIL_CONTENT = "테스트 내용입니다.";
    public static final String TEST_CATEGORY = "BACKEND";
    public static final List<String> TEST_TAGS = Arrays.asList("Spring", "Java");

    // 페이징 관련 상수
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;

    // 테스트 카운트 관련 상수
    public static final int TEST_TILS_COUNT = 5;

    // AI 관련 상수
    public static final String AI_RESPONSE_CONTENT = "# 로그인 기능 구현\n\n로그인 기능을 구현했습니다.";
    public static final String AI_HEALTH_OK = "OK";
    public static final String CONNECTION_FAILED_MESSAGE = "Connection failed";
    public static final List<String> AI_KEYWORDS = Arrays.asList("로그인", "인증", "스프링");

    // Git 관련 상수
    public static final String REPO_ID = "123";
    public static final String BRANCH_NAME = "main";
    public static final String COMMIT_TITLE = "로그인 기능 구현";
    public static final String GITHUB_USERNAME = "testuser";
    public static final Long GITHUB_ORG_ID = 1L;
    public static final Long GITHUB_REPO_ID = 1L;
    public static final String GITHUB_BRANCH = "main";
    public static final String COMMIT_SHA = "abc123def456";
    public static final String INVALID_DATE_FORMAT = "2024-13-45";

    // 날짜 관련 상수
    public static final LocalDate TEST_DATE = LocalDate.now().minusDays(1);

    // 에러 메시지 상수
    public static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";
    public static final String TIL_NOT_FOUND_MESSAGE = "TIL을 찾을 수 없습니다.";
    public static final String TIL_ALREADY_DELETED_MESSAGE = "이미 삭제된 TIL입니다.";
    public static final String TIL_ACCESS_DENIED_MESSAGE = "본인의 TIL만 접근할 수 있습니다.";
}
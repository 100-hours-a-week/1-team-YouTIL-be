package com.youtil.Constants;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * TIL 테스트용 상수 클래스
 * 모든 TIL 테스트에서 사용되는 상수들을 중앙 집중 관리
 */
public final class TilTestConstants {

    public static final long MOCK_TIL_ID = 1L;
    public static final String MOCK_TITLE = "title";
    public static final String MOCK_CONTENT = "content";
    public static final List<String> MOCK_TAGS = List.of("tag1", "tag2");
    public static final String MOCK_CATEGORY = "FULLSTACK";
    public static final int INITIAL_COMMENTS_COUNT = 0;
    public static final int INITIAL_VISITED_COUNT = 0;
    public static final boolean IS_DISPLAYED = true;
    public static final int INITIAL_RECOMMEND_COUNT = 0;

    // ======================== 사용자 관련 ========================
    public static final Long TEST_USER_ID = 1L;
    public static final Long ANOTHER_USER_ID = 2L;
    public static final Long NON_EXISTENT_USER_ID = 999L;
    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final String TEST_USER_NICKNAME = "testUser";
    public static final String TEST_USER_PROFILE_IMAGE = "profile.jpg";
    public static final String TEST_USER_DESCRIPTION = "테스트 사용자";

    // ======================== TIL 관련 ========================
    public static final Long TEST_TIL_ID = 1L;
    public static final Long ANOTHER_TIL_ID = 2L;
    public static final Long NON_EXISTENT_TIL_ID = 999L;
    public static final String TEST_TIL_TITLE = "Spring Boot 학습";
    public static final String TEST_TIL_CONTENT = "오늘은 Spring Boot를 공부했다.";
    public static final String TEST_TIL_CATEGORY = "BACKEND";
    public static final List<String> TEST_TIL_TAGS = Arrays.asList("Spring", "Java", "Backend");
    public static final String TEST_TIL_REPOSITORY = "test-repo";

    // 수정 관련
    public static final String UPDATED_TIL_TITLE = "수정된 Spring Boot 학습";
    public static final String LONG_TITLE_41_CHARS = "이것은41자를초과하는매우긴제목입니다테스트용으로작성된긴제목";
    public static final String VALID_TITLE_40_CHARS = "이것은정확히40자인제목입니다테스트용으로작성된제목이에용";

    // ======================== AI 요청/응답 관련 ========================
    public static final String AI_GENERATED_CONTENT = "# AI가 생성한 TIL 내용\n\n테스트 내용입니다.";
    public static final List<String> AI_GENERATED_KEYWORDS = Arrays.asList("Java", "Spring", "테스트");

    // ======================== GitHub 관련 ========================
    public static final String GITHUB_TOKEN = "github_test_token";
    public static final String GITHUB_REPOSITORY_CONFIG = "org:12345:main";
    public static final String GITHUB_FILE_URL = "https://github.com/test/repo/blob/main/tils/2025-01-15.md";
    public static final String GITHUB_COMMIT_SHA = "abc123def456";
    public static final String GITHUB_FILE_PATH = "tils/2025-01-15.md";
    public static final Long GITHUB_REPOSITORY_ID = 123L;
    public static final String GITHUB_BRANCH = "main";

    // ======================== 커밋 관련 ========================
    public static final String COMMIT_SHA = "abc123def456";
    public static final String COMMIT_MESSAGE = "feat: 로그인 기능 구현";
    public static final String COMMIT_USERNAME = "testuser";
    public static final String COMMIT_DATE = "2025-01-15";

    // ======================== 카운트 관련 ========================
    public static final int DEFAULT_RECOMMEND_COUNT = 0;
    public static final int DEFAULT_VISITED_COUNT = 0;
    public static final int DEFAULT_COMMENTS_COUNT = 0;
    public static final int INCREASED_VISITED_COUNT = 1;

    // ======================== 날짜 관련 ========================
    public static final LocalDate TEST_DATE = LocalDate.of(2025, 1, 15);
    public static final String TEST_DATE_STRING = "2025-01-15";
    public static final String INVALID_DATE_FORMAT = "2025/01/15";
    public static final int TEST_YEAR = 2025;
    public static final OffsetDateTime TEST_OFFSET_DATETIME = OffsetDateTime.now();

    // ======================== 페이징 관련 ========================
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 10;
    public static final int EMPTY_LIST_SIZE = 0;

    // ======================== HTTP 상태 코드 ========================
    public static final String HTTP_200 = "200";
    public static final String HTTP_201 = "201";
    public static final String HTTP_400 = "400";
    public static final String HTTP_401 = "401";
    public static final String HTTP_403 = "403";
    public static final String HTTP_404 = "404";
    public static final String HTTP_410 = "410";
    public static final String HTTP_500 = "500";
    public static final String HTTP_503 = "503";

    // ======================== 성공 메시지 ========================
    public static final String SUCCESS_TIL_CREATED = "TIL이 성공적으로 생성되었습니다.";
    public static final String SUCCESS_TIL_UPDATED = "TIL 제목이 성공적으로 수정되었습니다.";
    public static final String SUCCESS_TIL_DELETED = "TIL 비활성화에 성공했습니다.";
    public static final String SUCCESS_TIL_UPLOADED = "TIL이 성공적으로 GitHub에 업로드되었습니다.";
    public static final String SUCCESS_TIL_LIST_FETCHED = "내 TIL 목록 조회 성공";
    public static final String SUCCESS_TIL_DETAIL_FETCHED = "내 TIL 상세 조회 성공";
    public static final String SUCCESS_AI_SERVER_HEALTH = "AI 서버는 정상입니다";

    // ======================== 에러 메시지 ========================
    public static final String ERROR_TIL_NOT_FOUND = "TIL을 찾을 수 없습니다.";
    public static final String ERROR_TIL_ALREADY_DELETED = "이미 삭제된 TIL입니다.";
    public static final String ERROR_TIL_ACCESS_DENIED = "본인의 TIL만 접근할 수 있습니다.";
    public static final String ERROR_TIL_EDIT_DENIED = "TIL 수정 권한이 없습니다.";
    public static final String ERROR_TIL_DELETE_DENIED = "TIL 삭제 권한이 없습니다.";
    public static final String ERROR_USER_NOT_FOUND = "해당하는 유저가 존재하지 않습니다.";
    public static final String ERROR_REPOSITORY_NOT_SET = "기본 업로드 레포지토리가 설정되지 않았습니다. 먼저 레포지토리를 설정해주세요.";
    public static final String ERROR_DATE_FORMAT_INVALID = "날짜 형식이 올바르지 않습니다. 'yyyy-MM-dd' 형식을 사용해주세요.";
    public static final String ERROR_AI_CONNECTION_FAILED = "AI 서버와의 연결이 원활하지 않습니다.";
    public static final String ERROR_AI_EMPTY_RESPONSE = "AI 서버에서 유효한 응답을 받지 못했습니다.";
    public static final String ERROR_AI_HEALTH_FAILED = "AI 서버가 닫혀있습니다.";
    public static final String ERROR_TIL_CREATION_FAILED = "TIL 생성 중 오류가 발생했습니다.";

    // ======================== 필수 필드 에러 메시지 ========================
    public static final String ERROR_REPOSITORY_ID_REQUIRED = "레포지토리 ID가 필요합니다.";
    public static final String ERROR_BRANCH_REQUIRED = "브랜치명이 필요합니다.";
    public static final String ERROR_COMMITS_REQUIRED = "최소 하나 이상의 커밋 정보가 필요합니다.";
    public static final String ERROR_TITLE_REQUIRED = "TIL 제목이 필요합니다.";
    public static final String ERROR_CATEGORY_REQUIRED = "TIL 카테고리가 필요합니다.";
    public static final String ERROR_SHARED_STATUS_REQUIRED = "커뮤니티 업로드 여부가 필요합니다.";

    // ======================== AI 서버 URL ========================
    public static final String PRIMARY_AI_SERVER_URL = "http://primary-ai.test";
    public static final String SECONDARY_AI_SERVER_URL = "http://secondary-ai.test";
    public static final String AI_HEALTH_ENDPOINT = "/health";
    public static final String AI_TIL_ENDPOINT = "/til";

    private TilTestConstants() {
        // 인스턴스 생성 방지
    }
}

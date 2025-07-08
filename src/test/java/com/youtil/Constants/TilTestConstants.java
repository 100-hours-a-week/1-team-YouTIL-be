package com.youtil.Constants;

import java.util.Arrays;
import java.util.List;

public final class TilTestConstants {

    // 테스트 사용자 정보
    public static final Long TEST_USER_ID = 1L;
    public static final String TEST_USER_EMAIL = "test@example.com";
    public static final String TEST_USER_NICKNAME = "testUser";
    public static final String TEST_USER_PROFILE_IMAGE = "profile.jpg";
    public static final String TEST_USER_DESCRIPTION = "테스트 사용자";

    // 테스트 TIL 정보
    public static final Long TEST_TIL_ID = 1L;
    public static final Long NON_EXISTENT_TIL_ID = 999L;
    public static final String TEST_TIL_TITLE = "Spring Boot 학습";
    public static final String TEST_TIL_CONTENT = "오늘은 Spring Boot를 공부했다.";
    public static final String TEST_TIL_CATEGORY = "BACKEND";
    public static final List<String> TEST_TIL_TAGS = Arrays.asList("Spring", "Java", "Backend");
    public static final String TEST_TIL_REPOSITORY = "test-repo";

    // 수정 테스트 관련
    public static final String UPDATED_TIL_TITLE = "수정된 Spring Boot 학습";
    public static final String LONG_TITLE_41_CHARS = "이것은41자를초과하는매우긴제목입니다테스트용으로작성된긴제목";
    public static final String VALID_TITLE_40_CHARS = "이것은정확히40자인제목입니다테스트용으로작성된제목이에용";

    // GitHub 업로드 테스트 관련
    public static final String GITHUB_TOKEN = "github_test_token";
    public static final String GITHUB_REPOSITORY_CONFIG = "org:12345:main";
    public static final String GITHUB_FILE_URL = "https://github.com/test/repo/blob/main/tils/2025-01-15.md";
    public static final String GITHUB_COMMIT_SHA = "abc123def456";
    public static final String GITHUB_FILE_PATH = "tils/2025-01-15.md";

    // 카운트 관련
    public static final int DEFAULT_RECOMMEND_COUNT = 0;
    public static final int DEFAULT_VISITED_COUNT = 0;
    public static final int DEFAULT_COMMENTS_COUNT = 0;

    // 에러 메시지
    public static final String ERROR_TIL_NOT_FOUND = "TIL을 찾을 수 없습니다.";
    public static final String ERROR_TIL_ALREADY_DELETED = "이미 삭제된 TIL입니다.";
    public static final String ERROR_TIL_ACCESS_DENIED = "본인의 TIL만 접근할 수 있습니다.";
    public static final String ERROR_TIL_EDIT_DENIED = "TIL 수정 권한이 없습니다.";
    public static final String ERROR_TIL_DELETE_DENIED = "TIL 삭제 권한이 없습니다.";
    public static final String ERROR_REPOSITORY_NOT_SET = "기본 업로드 레포지토리가 설정되지 않았습니다. 먼저 레포지토리를 설정해주세요.";

    // HTTP 상태 코드
    public static final String HTTP_200 = "200";
    public static final String HTTP_400 = "400";
    public static final String HTTP_401 = "401";
    public static final String HTTP_403 = "403";
    public static final String HTTP_404 = "404";
    public static final String HTTP_410 = "410";
    public static final String HTTP_500 = "500";

    // 성공 메시지
    public static final String SUCCESS_TIL_UPDATED = "TIL 제목이 성공적으로 수정되었습니다.";
    public static final String SUCCESS_TIL_UPLOADED = "TIL이 성공적으로 GitHub에 업로드되었습니다.";

    private TilTestConstants() {
    }
}

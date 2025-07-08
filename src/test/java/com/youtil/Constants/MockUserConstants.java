package com.youtil.Constants;

public class MockUserConstants {

    public static final long MOCK_USER_ID = 1L;
    public static final String MOCK_USER_EMAIL = "test@email.com";
    public static final String MOCK_GITHUB_TOKEN = "accessToken";
    public static final String MOCK_USER_NICKNAME = "jun";
    public static final String MOCK_USER_PROFILE = "profileImageUrl";

    // 테스트에서 재사용할 추가 상수들
    public static final long OTHER_USER_ID = 2L;
    public static final long INVALID_USER_ID = 999L;
    public static final String MOCK_USER_NICKNAME_2 = "testuser2";
    public static final String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";

    private MockUserConstants() {
    }
}
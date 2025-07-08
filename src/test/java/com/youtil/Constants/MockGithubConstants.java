package com.youtil.Constants;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MockGithubConstants {

    // 공통 상수
    public static final Long MOCK_USER_ID = 1L;
    public static final Long MOCK_REPOSITORY_ID = 12345L;
    public static final Long MOCK_ORGANIZATION_ID = 999L;
    public static final String MOCK_BRANCH = "main";
    public static final String MOCK_USERNAME = "test-user";
    public static final String MOCK_REPO_NAME = "test-repo";
    public static final String MOCK_OWNER = "test-owner";
    public static final String MOCK_ENCRYPTED_TOKEN = "encrypted-token";
    public static final String MOCK_DECRYPTED_TOKEN = "decrypted-token";

    // User 관련 상수
    public static final String MOCK_USER_EMAIL = "test@email.com";
    public static final String MOCK_USER_NICKNAME = "jun";
    public static final String MOCK_USER_PROFILE = "profileImageUrl";

    // 날짜 관련
    public static final String MOCK_DATE = "2024-01-15";
    public static final LocalDate MOCK_START_DATE = LocalDate.of(2024, 1, 1);
    public static final LocalDate MOCK_END_DATE = LocalDate.of(2024, 12, 31);

    // 레포지토리 설정 관련
    public static final String MOCK_UPLOAD_REPOSITORY_CONFIG = "12345/main";
    public static final String MOCK_ORG_UPLOAD_REPOSITORY_CONFIG = "999/12345/main";
    // 에러 케이스용 상수
    public static final Long INVALID_USER_ID = 99999L;
    public static final Long INVALID_REPOSITORY_ID = 99999L;

    private MockGithubConstants() {}

    public static Map<String, Object> createMockRepoInfo() {
        Map<String, Object> repoInfo = new HashMap<>();
        repoInfo.put("name", MOCK_REPO_NAME);
        Map<String, Object> owner = new HashMap<>();
        owner.put("login", MOCK_OWNER);
        repoInfo.put("owner", owner);
        return repoInfo;
    }

    public static Map<String, Object> createMockOrgRepoInfo() {
        Map<String, Object> repoInfo = new HashMap<>();
        repoInfo.put("name", "org-repo");
        Map<String, Object> owner = new HashMap<>();
        owner.put("login", "test-org");
        repoInfo.put("owner", owner);
        return repoInfo;
    }
}

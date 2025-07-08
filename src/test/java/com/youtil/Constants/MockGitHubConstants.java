package com.youtil.Constants;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class MockGitHubConstants {

    // Organization 관련
    public static final Long ORG_ID_1 = 12345L;
    public static final Long ORG_ID_2 = 67890L;
    public static final Long INVALID_ORG_ID = 99999L;
    public static final String ORG_LOGIN_1 = "test-org-1";
    public static final String ORG_LOGIN_2 = "test-org-2";
    public static final String ORG_AVATAR_URL_1 = "https://avatars.githubusercontent.com/u/12345";
    public static final String ORG_AVATAR_URL_2 = "https://avatars.githubusercontent.com/u/67890";
    public static final String ORG_DESCRIPTION_1 = "Test Organization 1";
    public static final String ORG_DESCRIPTION_2 = "Test Organization 2";

    // Repository 관련
    public static final Long REPO_ID = 123L;
    public static final Long INVALID_REPO_ID = 999L;
    public static final String REPO_NAME_1 = "test-repo";
    public static final String REPO_FULL_NAME_1 = "test-org-1/test-repo";
    public static final String REPO_DESCRIPTION_1 = "Test Repository";

    public static final Long PERSONAL_REPO_ID = 222L;
    public static final String PERSONAL_REPO_NAME = "personal-repo";
    public static final String PERSONAL_REPO_FULL_NAME = "testuser/personal-repo";
    public static final String PERSONAL_REPO_DESCRIPTION = "Personal Repository";

    // Branch 관련
    public static final String BRANCH_MAIN = "main";
    public static final String BRANCH_DEVELOP = "develop";

    // Commit 관련
    public static final String COMMIT_SHA_1 = "abc123def456";
    public static final String COMMIT_SHA_2 = "def456ghi789";
    public static final String INVALID_COMMIT_SHA = "invalid-sha";
    public static final String COMMIT_MESSAGE_1 = "feat: add new feature";
    public static final String COMMIT_MESSAGE_2 = "fix: fix bug";
    public static final String COMMIT_URL_1 = "https://api.github.com/repos/test/test/commits/abc123def456";
    public static final String COMMIT_URL_2 = "https://api.github.com/repos/test/test/commits/def456ghi789";
    public static final String COMMIT_TITLE = "Daily commit summary";

    // GitHub User 관련
    public static final String OTHER_USERNAME = "otheruser";
    public static final String OTHER_EMAIL = "other@example.com";
    public static final String GITHUB_USERNAME = "testuser";
    public static final Long GITHUB_REPO_ID = 123L;
    public static final String GITHUB_BRANCH = "main";

    // 페이지네이션 관련
    public static final Integer GIT_DEFAULT_PAGE = 1;
    public static final Integer GIT_DEFAULT_SIZE = 30;

    // GitHub 에러 메시지
    public static final String GITHUB_TOKEN_INVALID_MESSAGE = "GitHub token is invalid or missing";
    public static final String REPO_ID_REQUIRED_MESSAGE = "Repository ID is required";
    public static final String REQUIRED_PARAMETER_MESSAGE = "Required parameter is missing";
    public static final String CONNECTION_FAILED_MESSAGE = "Connection failed";
}
package com.youtil.Mock;

import java.util.Map;
import static com.youtil.Constants.MockUserConstants.*;
import static com.youtil.Constants.MockGitHubConstants.*;
import static com.youtil.Constants.MockTilConstants.TEST_DATE;

/**
 * GitHub API 응답 Mock 데이터 생성용 Builder
 */
public class MockGitHubBuilder {

    /**
     * GitHub User 정보 응답 데이터
     */
    public static Map<String, Object> createUserInfoResponse() {
        return Map.of(
                "login", MOCK_USER_NICKNAME,
                "id", MOCK_USER_ID,
                "type", "User",
                "name", MOCK_USER_NICKNAME,
                "email", MOCK_USER_EMAIL,
                "avatar_url", MOCK_USER_PROFILE,
                "html_url", "https://github.com/" + MOCK_USER_NICKNAME
        );
    }

    /**
     * Repository 메타데이터 (Owner 정보 포함)
     */
    public static Map<String, Object> createRepositoryWithOwner() {
        return Map.of(
                "id", REPO_ID,
                "name", REPO_NAME_1,
                "full_name", REPO_FULL_NAME_1,
                "description", REPO_DESCRIPTION_1,
                "private", false,
                "owner", Map.of(
                        "login", MOCK_USER_NICKNAME,
                        "id", MOCK_USER_ID,
                        "type", "User",
                        "avatar_url", MOCK_USER_PROFILE,
                        "html_url", "https://github.com/" + MOCK_USER_NICKNAME
                )
        );
    }

    /**
     * Repository 기본 메타데이터
     */
    public static Map<String, Object> createRepositoryBasic() {
        return Map.of(
                "id", REPO_ID,
                "name", REPO_NAME_1,
                "full_name", REPO_FULL_NAME_1,
                "owner", Map.of(
                        "login", MOCK_USER_NICKNAME,
                        "id", MOCK_USER_ID,
                        "type", "User"
                )
        );
    }

    /**
     * 조직 목록 응답 데이터
     */
    public static Map<String, Object>[] createOrganizationsResponse() {
        return new Map[]{
                Map.of("id", ORG_ID_1, "login", ORG_LOGIN_1),
                Map.of("id", ORG_ID_2, "login", ORG_LOGIN_2)
        };
    }

    /**
     * 레포지토리 목록 응답 데이터
     */
    public static Map<String, Object>[] createRepositoriesResponse() {
        return new Map[]{
                Map.of("id", REPO_ID, "name", REPO_NAME_1, "full_name", REPO_FULL_NAME_1)
        };
    }

    /**
     * 개인 레포지토리 목록 응답 데이터
     */
    public static Map<String, Object>[] createPersonalRepositoriesResponse() {
        return new Map[]{
                Map.of("id", PERSONAL_REPO_ID, "name", PERSONAL_REPO_NAME, "full_name", PERSONAL_REPO_FULL_NAME)
        };
    }

    /**
     * 브랜치 목록 응답 데이터
     */
    public static Map<String, Object>[] createBranchesResponse() {
        return new Map[]{
                Map.of("name", BRANCH_MAIN),
                Map.of("name", BRANCH_DEVELOP)
        };
    }

    /**
     * 단일 브랜치 응답 데이터
     */
    public static Map<String, Object>[] createSingleBranchResponse() {
        return new Map[]{
                Map.of("name", BRANCH_MAIN)
        };
    }

    /**
     * 커밋 정보 (단일)
     */
    public static Map<String, Object> createCommitInfo(String sha, String message) {
        return Map.of(
                "sha", sha,
                "commit", Map.of(
                        "message", message,
                        "committer", Map.of("date", TEST_DATE.toString() + "T10:00:00Z")
                ),
                "author", Map.of("login", MOCK_USER_NICKNAME)
        );
    }

    /**
     * 커밋 목록 응답 데이터
     */
    public static Map<String, Object>[] createCommitsResponse() {
        return new Map[]{
                createCommitInfo(COMMIT_SHA_1, COMMIT_MESSAGE_1),
                createCommitInfo(COMMIT_SHA_2, COMMIT_MESSAGE_2)
        };
    }

    /**
     * 필터링된 커밋 목록 (사용자 커밋만)
     */
    public static Map<String, Object>[] createFilteredCommitsResponse() {
        return new Map[]{
                createCommitInfo(COMMIT_SHA_1, COMMIT_MESSAGE_1)
        };
    }

    /**
     * 커밋 상세 정보 (파일 변경사항 포함)
     */
    public static Map<String, Object> createCommitDetailInfo() {
        return Map.of(
                "sha", COMMIT_SHA_1,
                "commit", Map.of(
                        "message", COMMIT_MESSAGE_1,
                        "committer", Map.of("date", TEST_DATE.toString() + "T10:00:00Z")
                ),
                "author", Map.of("login", MOCK_USER_NICKNAME),
                "files", java.util.List.of(
                        Map.of(
                                "filename", "src/Main.java",
                                "patch", "System.out.println(\"Hello world\");",
                                "status", "modified"
                        ),
                        Map.of(
                                "filename", "src/Utils.java",
                                "patch", "public void utilMethod() {}",
                                "status", "added"
                        )
                )
        );
    }

    /**
     * 빈 응답 데이터들
     */
    public static Map<String, Object>[] createEmptyResponse() {
        return new Map[0];
    }
}

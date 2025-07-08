package com.youtil.Api.Github.Constants;

public final class GitHubApiConstants {

    private GitHubApiConstants() {}

    public static final String BASE_URL = "https://api.github.com";

    // User 관련 URL
    public static final String USER_INFO_URL = "/user";
    public static final String USER_ORGS_URL = "/user/orgs";
    public static final String USER_REPOS_URL = "/user/repos";
    public static final String USER_TEAMS_URL = "/user/teams";

    // Repository 관련 URL
    public static final String REPOSITORIES_BASE_URL = "/repositories/";
    public static final String REPOS_BASE_URL = "/repos/";
    public static final String TEAMS_BASE_URL = "/teams/";

    // URL 서브 경로
    public static final String BRANCHES_PATH = "/branches";
    public static final String COMMITS_PATH = "/commits";
    public static final String CONTENTS_PATH = "/contents/";
    public static final String REPOS_PATH = "/repos";
}

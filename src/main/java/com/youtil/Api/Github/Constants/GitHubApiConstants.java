package com.youtil.Api.Github.Constants;

public final class GitHubApiConstants {

    private GitHubApiConstants() {}

    public static final String BASE_URL = "https://api.github.com";

    // User 관련 URL
    public static final String USER_INFO_URL = BASE_URL + "/user";
    public static final String USER_ORGS_URL = BASE_URL + "/user/orgs";
    public static final String USER_REPOS_URL = BASE_URL + "/user/repos";
    public static final String USER_TEAMS_URL = BASE_URL + "/user/teams";

    // Repository 관련 URL
    public static final String REPOSITORIES_BASE_URL = BASE_URL + "/repositories/";
    public static final String REPOS_BASE_URL = BASE_URL + "/repos/";
    public static final String TEAMS_BASE_URL = BASE_URL + "/teams/";

    // URL 서브 경로
    public static final String BRANCHES_PATH = "/branches";
    public static final String COMMITS_PATH = "/commits";
    public static final String CONTENTS_PATH = "/contents/";
    public static final String REPOS_PATH = "/repos";

    // 쿼리 파라미터
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_PER_PAGE = "per_page";
    public static final String PARAM_SHA = "sha";
    public static final String PARAM_SINCE = "since";
    public static final String PARAM_UNTIL = "until";
    public static final String PARAM_AUTHOR = "author";
    public static final String PARAM_AFFILIATION = "affiliation";
    public static final String PARAM_REF = "ref";

    // 기본값
    public static final String AFFILIATION_OWNER = "owner";
    public static final String AFFILIATION_OWNER_COLLABORATOR = "owner,collaborator";
}

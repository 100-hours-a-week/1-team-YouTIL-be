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
    public static final String REPOSITORIES_URL = BASE_URL + "/repositories/%d";
    public static final String REPO_BRANCHES_URL = BASE_URL + "/repos/%s/%s/branches";
    public static final String REPO_COMMITS_URL = BASE_URL + "/repos/%s/%s/commits";
    public static final String REPO_COMMIT_DETAIL_URL = BASE_URL + "/repos/%s/%s/commits/%s";
    public static final String REPO_CONTENTS_URL = BASE_URL + "/repos/%s/%s/contents/%s";

    // Organization 관련 URL
    public static final String ORG_REPOS_URL = BASE_URL + "/orgs/%s/repos";

    // Teams 관련 URL
    public static final String TEAM_REPOS_URL = BASE_URL + "/teams/%d/repos";

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

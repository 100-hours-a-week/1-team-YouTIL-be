package com.youtil.Api.Github.Constants;

public final class GitHubApiConstants {

    private GitHubApiConstants() {}

    public static final String BASE_URL = "https://api.github.com";

    public static final String USER_INFO_URL = BASE_URL + "/user";
    public static final String USER_ORGS_URL = BASE_URL + "/user/orgs";
    public static final String REPOSITORIES_URL = BASE_URL + "/repositories/%d";

    public static final String PARAM_PAGE = "page";
    public static final String PARAM_PER_PAGE = "per_page";
}

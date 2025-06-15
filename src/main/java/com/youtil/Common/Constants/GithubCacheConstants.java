package com.youtil.Common.Constants;

import java.time.Duration;

public class GithubCacheConstants {

    // Cache Keys
    public static final String GITHUB_CACHE_PREFIX = "github:";
    public static final String ORG_CACHE_KEY = GITHUB_CACHE_PREFIX + "orgs:";
    public static final String REPO_CACHE_KEY = GITHUB_CACHE_PREFIX + "repos:";
    public static final String BRANCH_CACHE_KEY = GITHUB_CACHE_PREFIX + "branches:";
    public static final String COMMIT_CACHE_KEY = GITHUB_CACHE_PREFIX + "commits:";
    public static final String COMMIT_DETAIL_CACHE_KEY = GITHUB_CACHE_PREFIX + "commit_details:";

    // TTL Settings (GitHub 데이터 특성에 맞춰 설정)
    public static final Duration ORG_CACHE_TTL = Duration.ofHours(6);      // 조직 목록
    public static final Duration REPO_CACHE_TTL = Duration.ofHours(3);     // 레포 목록
    public static final Duration BRANCH_CACHE_TTL = Duration.ofHours(1);    // 브랜치 목록
    public static final Duration COMMIT_CACHE_TTL = Duration.ofMinutes(30); // 커밋 목록
    public static final Duration COMMIT_DETAIL_CACHE_TTL = Duration.ofHours(2); // 커밋 상세
}

package com.youtil.Api.User.Constants;

import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class UserServiceConstants {

    //create User 관련 상수
    public static final long MOCK_USER_ID = 1L;
    public static final String MOCK_USER_EMAIL = "test@email.com";
    public static final String MOCK_GITHUB_TOKEN = "accessToken";
    public static final String MOCK_USER_NICKNAME = "jun";
    public static final String MOCK_USER_PROFILE = "profileImageUrl";
    //create Till 관련 상수
    public static final long MOCK_TIL_ID = 1L;
    public static final String MOCK_TITLE = "title";
    public static final String MOCK_CONTENT = "content";
    public static final List<String> MOCK_TAGS = List.of("tag1", "tag2");
    public static final String MOCK_CATEGORY = "FULLSTACK";
    public static final int INITIAL_COMMENTS_COUNT = 0;
    public static final int INITIAL_VISITED_COUNT = 0;
    public static final boolean IS_DISPLAYED = true;
    public static final int INITIAL_RECOMMEND_COUNT = 0;
    //Login Test 관련 상수
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String ORIGIN = "localhost";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String ENCRYPTED_TOKEN = "encryptedToken";
    public static final String JWT_ACCESS_TOKEN = "jwtAccessToken";
    public static final String JWT_REFRESH_TOKEN = "jwtRefreshToken";

    //GitHub APP 관련 상수

    public static final String MOCK_CLIENT_ID = "mockClientId";
    public static final String MOCK_CLIENT_SECRET = "mockClientSecret";
    public static final String EMAIL_URI = "https://api.github.com/user/emails";
    public static final String USER_SPEC_URI = "https://api.github.com/user";

    //Pageable
    public static final Pageable PAGEABLE = PageRequest.of(0, 20);

    private UserServiceConstants() {
    }
}

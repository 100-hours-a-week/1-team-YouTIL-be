package com.youtil.Constants;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public class UserServiceConstants {

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

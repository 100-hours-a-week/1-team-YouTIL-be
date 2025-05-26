package com.youtil.Mock;

import com.youtil.Common.Enums.Status;
import static com.youtil.Constants.MockUserConstants.MOCK_GITHUB_TOKEN;
import static com.youtil.Constants.MockUserConstants.MOCK_USER_EMAIL;
import static com.youtil.Constants.MockUserConstants.MOCK_USER_ID;
import static com.youtil.Constants.MockUserConstants.MOCK_USER_NICKNAME;
import static com.youtil.Constants.MockUserConstants.MOCK_USER_PROFILE;
import com.youtil.Model.User;

public class MockUserBuilder {

    public static User createMockUser() {
        return User.builder()
                .id(MOCK_USER_ID)
                .email(MOCK_USER_EMAIL)
                .status(Status.active)
                .githubToken(MOCK_GITHUB_TOKEN)
                .nickname(MOCK_USER_NICKNAME)
                .profileImageUrl(MOCK_USER_PROFILE)
                .build();
    }
}

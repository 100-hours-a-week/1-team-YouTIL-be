package com.youtil.Mock;

import com.youtil.Api.Github.Dto.GitHubRepositorySettingDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.User;
import static com.youtil.Constants.MockGithubConstants.*;


public class MockGithubBuilder {

    public static User createMockUserWithGithub() {
        return User.builder()
                .id(MOCK_USER_ID)
                .email(MOCK_USER_EMAIL)
                .status(Status.active)
                .githubToken(MOCK_ENCRYPTED_TOKEN)
                .nickname(MOCK_USER_NICKNAME)
                .profileImageUrl(MOCK_USER_PROFILE)
                .uploadRepository(MOCK_UPLOAD_REPOSITORY_CONFIG)
                .build();
    }

    public static User createMockUserWithOrgRepo() {
        return User.builder()
                .id(MOCK_USER_ID)
                .email(MOCK_USER_EMAIL)
                .status(Status.active)
                .githubToken(MOCK_ENCRYPTED_TOKEN)
                .nickname(MOCK_USER_NICKNAME)
                .profileImageUrl(MOCK_USER_PROFILE)
                .uploadRepository(MOCK_ORG_UPLOAD_REPOSITORY_CONFIG)
                .build();
    }

    public static User createMockUserWithoutConfig() {
        return User.builder()
                .id(MOCK_USER_ID)
                .email(MOCK_USER_EMAIL)
                .status(Status.active)
                .githubToken(MOCK_ENCRYPTED_TOKEN)
                .nickname(MOCK_USER_NICKNAME)
                .profileImageUrl(MOCK_USER_PROFILE)
                .uploadRepository(null)
                .build();
    }

    public static User createMockUserWithoutToken() {
        return User.builder()
                .id(MOCK_USER_ID)
                .email(MOCK_USER_EMAIL)
                .status(Status.active)
                .githubToken(null)
                .nickname(MOCK_USER_NICKNAME)
                .profileImageUrl(MOCK_USER_PROFILE)
                .build();
    }

    public static GitHubRepositorySettingDTO.SetRepositoryRequest createPersonalRepoRequest() {
        return GitHubRepositorySettingDTO.SetRepositoryRequest.builder()
                .repositoryId(MOCK_REPOSITORY_ID)
                .branch(MOCK_BRANCH)
                .build();
    }

    public static GitHubRepositorySettingDTO.SetRepositoryRequest createOrgRepoRequest() {
        return GitHubRepositorySettingDTO.SetRepositoryRequest.builder()
                .organizationId(MOCK_ORGANIZATION_ID)
                .repositoryId(MOCK_REPOSITORY_ID)
                .branch(MOCK_BRANCH)
                .build();
    }
}

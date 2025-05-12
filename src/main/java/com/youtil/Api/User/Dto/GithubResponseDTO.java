package com.youtil.Api.User.Dto;

import lombok.Getter;

public class GithubResponseDTO {
    @Getter
    public static class GitHubAccessTokenResponse{
        private String access_token;
        private String scope;
        private String token_type;

    }
    @Getter
    public static class GitHubUserInfo {
        private String login; // 유저 깃허브 아이디
        private String avatar_url; // 프로필 사진

    }

    @Getter
    public static class GitHubEmailInfo {
        private String email;
        private boolean primary;
        private boolean verified;
    }
}

package com.youtil.Api.User.Dto;

import lombok.Getter;

public class GithubResponseDTO {
    @Getter
    public static class GitHubAccessTokenResponse{
        private String accessToken;
        private String scope;
        private String tokenType;

    }
}

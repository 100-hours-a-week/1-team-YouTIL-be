package com.youtil.Api.User.Dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class GitHubRequestDTO {
    @Getter
    @Builder
    public static class GitHubAccessTokenRequest {
        private String client_id;
        private String client_secret;
        private String code;



    }
}

package com.youtil.Api.User.Dto;

import lombok.Builder;
import lombok.Getter;

public class UserResponseDTO {
    @Getter
    @Builder
    public static class LoginResponseDTO {
        private String accessToken;
        private String refreshToken;
    }

}

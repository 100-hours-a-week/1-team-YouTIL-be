package com.youtil.Api.User.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

public class UserRequestDTO {

    @Getter
    public static class LoginRequestDTO {

        @Schema(description = "깃허브 인가 코드", example = "1jjdwoqjdoxjcv")
        String authorizationCode;
    }

    @Getter
    public static class EditUserProfileRequestDTO {
        @Schema(description = "유저 소개말", example = "안녕하세요")
        private String description;
        @Schema(description = "유저 프로필 url", example = "https://profle")
        private String profileImageUrl;
    }
}

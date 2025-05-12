package com.youtil.Api.User.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

public class UserRequestDTO {

    @Getter
    public static class LoginRequestDTO {

        @Schema(description = "깃허브 인가 코드", example = "1jjdwoqjdoxjcv")
        String authorizationCode;
    }
}

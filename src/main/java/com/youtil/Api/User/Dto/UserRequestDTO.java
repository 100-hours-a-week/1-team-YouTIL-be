package com.youtil.Api.User.Dto;

import lombok.Getter;

public class UserRequestDTO {
    @Getter
    public static class LoginRequestDTO{
        String authorizationCode;
    }
}

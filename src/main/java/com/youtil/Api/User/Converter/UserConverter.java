package com.youtil.Api.User.Converter;

import com.youtil.Api.User.Dto.GithubResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Common.Enums.UserStatus;
import com.youtil.Model.User;

public class UserConverter {
    public static User toUser(String email, GithubResponseDTO.GitHubUserInfo gitHubUserInfo,String accessToken) {
        return User.builder()
                .email(email)
                .githubToken(accessToken)
                .status(UserStatus.active)
                .profileImageUrl(gitHubUserInfo.getAvatar_url())
                .nickname(gitHubUserInfo.getLogin())
                .build();
    }
    public static UserResponseDTO.LoginResponseDTO toUserResponseDTO(String accessToken,String refreshToken) {
        return UserResponseDTO.LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}

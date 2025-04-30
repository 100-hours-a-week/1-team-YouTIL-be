package com.youtil.Api.User.Converter;

import com.youtil.Api.User.Dto.GithubResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO.GetUserTilsResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO.TilListItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.User;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserConverter {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static User toUser(String email, GithubResponseDTO.GitHubUserInfo gitHubUserInfo,
            String accessToken) {
        return User.builder()
                .email(email)
                .githubToken(accessToken)
                .status(Status.active)
                .profileImageUrl(gitHubUserInfo.getAvatar_url())
                .nickname(gitHubUserInfo.getLogin())
                .build();
    }

    public static UserResponseDTO.LoginResponseDTO toUserResponseDTO(String accessToken,
            String refreshToken) {
        return UserResponseDTO.LoginResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public static UserResponseDTO.GetUserInfoResponseDTO toUserInfoResponseDTO(User user) {
        return UserResponseDTO.GetUserInfoResponseDTO.builder()
                .name(user.getNickname())
                .profileUrl(user.getProfileImageUrl())
                .description(user.getDescription())
                .build();
    }

    public static UserResponseDTO.GetUserTilsResponseDTO toUserTilsResponseDTO(
            List<TilListItem> tils) {
        return GetUserTilsResponseDTO.builder()
                .tils(tils)
                .build();
    }
}

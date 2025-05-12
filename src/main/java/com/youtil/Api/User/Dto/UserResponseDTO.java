package com.youtil.Api.User.Dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class UserResponseDTO {

    @Getter
    @Builder
    @Schema(description = "로그인 응답")
    public static class LoginResponseDTO {

        @Schema(description = "엑세스토큰")
        private String accessToken;
        @Schema(description = "리프레쉬토큰")
        private String refreshToken;
    }

    @Getter
    @Builder
    @Schema(description = "유저 정보 조회")
    public static class GetUserInfoResponseDTO {
        @Schema(description = "유저 Id,",example = "0")
        private Long userId;
        @Schema(description = "유저 이름", example = "jun")
        private String name;
        @Schema(description = "유저 프로필url", example = "jun")
        private String profileUrl;
        @Schema(description = "유저 자기소개", example = "안녕")
        private String description;
    }

    @Getter
    @Builder
    @Schema(description = "TIL 카운트 조회")
    public static class GetUserTilCountResponseDTO {

        @Schema(description = "연도", example = "2025")
        private int year;
        @Schema(description = "TIL 카운트 정보")
        private TilCountYearsItem tils;

    }

    @Getter
    @Builder
    public static class GetUserTilsResponseDTO {

        @Schema(description = "Til 리스트")
        private List<TilListItem> tils;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TilListItem {

        private long id;
        private String userName;
        private String userProfileImageUrl;
        private long tilId;
        private String title;
        private List<String> tags;
        private OffsetDateTime createdAt;


    }

    //아이템 Dto
    @Getter
    @Builder
    public static class TilCountYearsItem {

        @Schema(description = "1월", example = "[0,0,0,0]")
        private List<Integer> jan;
        @Schema(description = "2월", example = "[0,0,0,0]")
        private List<Integer> feb;
        @Schema(description = "3월", example = "[0,0,0,0]")
        private List<Integer> mar;
        @Schema(description = "4월", example = "[0,0,0,0]")
        private List<Integer> apr;
        @Schema(description = "5월", example = "[0,0,0,0]")
        private List<Integer> may;
        @Schema(description = "6월", example = "[0,0,0,0]")
        private List<Integer> jun;
        @Schema(description = "7월", example = "[0,0,0,0]")
        private List<Integer> jul;
        @Schema(description = "8월", example = "[0,0,0,0]")
        private List<Integer> aug;
        @Schema(description = "9월", example = "[0,0,0,0]")
        private List<Integer> sep;
        @Schema(description = "10월", example = "[0,0,0,0]")
        private List<Integer> oct;
        @Schema(description = "11월", example = "[0,0,0,0]")
        private List<Integer> nov;
        @Schema(description = "12월", example = "[0,0,0,0]")
        private List<Integer> dec;

    }
}

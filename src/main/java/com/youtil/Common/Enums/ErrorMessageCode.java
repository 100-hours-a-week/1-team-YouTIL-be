package com.youtil.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorMessageCode {
    USER_NOT_FOUND("유저를 찾을 수 없거나 탈퇴한 계정입니다."),
    USER_ALREADY_INACTIVE("유저가 탈퇴되어있는 상태입니다.");
    private final String message;
}

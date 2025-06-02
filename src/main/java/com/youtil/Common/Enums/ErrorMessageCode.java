package com.youtil.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorMessageCode {
    USER_NOT_FOUND("400", "유저를 찾을 수 없거나 탈퇴한 계정입니다."),
    USER_ALREADY_INACTIVE("400", "유저가 탈퇴되어있는 상태입니다."),

    IMAGE_UPLOAD_FAILED("500", "이미지 업로드에 실패했습니다."),
    NOT_MATCH_IMAGE("400", "이미지 파일 형식에 맞지 않습니다."),
    WRONG_AUTHORIZATION_CODE("500", "깃허브 인가 코드가 잘못되었습니다."),
    GITHUB_PROFILE_NOT_FOUND("500", "해당 깃허브 프로필 정보를 가져 올수 없습니다."),
    GITHUB_EMAIL_NOT_FOUND("500", "해당 깃허브 이메일 정보를 가져 올수 없습니다."),

    AI_SEVER_NOT_HEALTH("503", "인공지능 서버가 닫혀있습니다."),
    NOT_MATCH_INTERVIEW("400", "해당 면접질문의 소유자가 아닙니다."),
    INTERVIEW_NOT_FOUND("400", "면접 질문을 찾을 수 없거나 삭제된 면접질문입니다."),
    TIL_NOT_FOUND("400", "TIL을 찾을 수 없거나 삭제된 계정입니다."),


    REQUEST_VALUE_NULL("400","필수값들이 누락되어있습니다.");



    private final String code;
    private final String message;
}

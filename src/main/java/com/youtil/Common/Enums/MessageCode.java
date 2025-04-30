package com.youtil.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageCode {
    // 유저 관련 성공 메시지
    PROFILE_UPDATED("프로필 변경에 성공했습니다."),
    PASSWORD_UPDATED("비밀번호 변경에 성공했습니다."),
    USER_DEACTIVE("회원 탈퇴가 완료되었습니다."),
    
    // 게시물 관련 메시지
    POST_CREATED("게시물이 성공적으로 생성되었습니다."),
    POST_UPDATED("게시물 수정에 성공했습니다."),
    POST_DELETED("삭제에 성공했습니다."),

    // 댓글 관련 메시지
    COMMENT_CREATED("댓글이 성공적으로 등록되었습니다."),
    COMMENT_UPDATED("댓글 수정에 성공했습니다."),
    COMMENT_DELETED("댓글 삭제에 성공했습니다.");

    private final String message;
}

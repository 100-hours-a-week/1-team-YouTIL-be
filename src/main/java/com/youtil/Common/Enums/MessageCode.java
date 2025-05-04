package com.youtil.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageCode {
    // 유저 관련 성공 메시지
    PROFILE_UPDATED("프로필 변경에 성공했습니다."),
    PASSWORD_UPDATED("비밀번호 변경에 성공했습니다."),
    USER_DEACTIVE("유저 탈퇴에 성공했습니다!"),
    LOGIN_SUCCESS("로그인에 성공했습니다!"),
    FIND_USER_INFORMATION_SUCCESS("유저 조회에 성공했습니다!"),

    //til 관련 성공메시지
    FIND_USER_TILS__COUNT_SUCCESS("유저 TIL 기록 조회에 성공했습니다!"),
    FIND_USER_WRITE_TILS_SUCCESS("해당 유저가 작성한 TIL 조회에 성공했습니다!"),
    
    // 게시물 관련 메시지
    POST_CREATED("게시물이 성공적으로 생성되었습니다."),
    POST_UPDATED("게시물 수정에 성공했습니다."),
    POST_DELETED("삭제에 성공했습니다."),

    // 댓글 관련 메시지
    COMMENT_CREATED("댓글이 성공적으로 등록되었습니다."),
    COMMENT_UPDATED("댓글 수정에 성공했습니다."),
    COMMENT_DELETED("댓글 삭제에 성공했습니다."),


    //뉴스 관련 메시지
    FIND_NEWS_SUCCESS("뉴스 데이터를 성공적으로 불러왔습니다!"),

    //이미지 호스팅 관련 메시지
    UPLOAD_IMAGE_SUCCESS("이미지를 성공적으로 업로드했습니다!");

    private final String message;
}

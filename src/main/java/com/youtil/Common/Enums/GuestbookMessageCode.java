package com.youtil.Common.Enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GuestbookMessageCode {

    // 성공 메시지
    CREATE_GUESTBOOK_SUCCESS("방명록이 성공적으로 작성되었습니다.", "G001"),
    GET_GUESTBOOK_LIST_SUCCESS("방명록 목록을 성공적으로 조회했습니다.", "G002"),
    UPDATE_GUESTBOOK_SUCCESS("성공했습니다.", "G003"),
    DELETE_GUESTBOOK_SUCCESS("방명록이 성공적으로 삭제되었습니다.", "G004"),

    // 오류 메시지
    GUESTBOOK_NOT_FOUND("해당하는 방명록이 존재하지 않습니다.", "G101"),
    INVALID_GUESTBOOK_ACCESS("해당하는 유저가 존재하지 않거나, 유저 본인의 방명록이 아닙니다.", "G102"),
    INVALID_GUESTBOOK_CONTENT("방명록 내용이 올바르지 않습니다.", "G103"),
    GUESTBOOK_CONTENT_TOO_LONG("방명록 내용이 너무 깁니다. (최대 50자)", "G104"),
    GUESTBOOK_CONTENT_EMPTY("방명록 내용을 입력해주세요.", "G105"),
    INVALID_PARENT_GUESTBOOK("유효하지 않은 상위 방명록입니다.", "G106"),
    GUESTBOOK_REPLY_DEPTH_EXCEEDED("답글은 1단계까지만 가능합니다.", "G107"),
    CANNOT_REPLY_TO_DELETED_GUESTBOOK("삭제된 방명록에는 답글을 작성할 수 없습니다.", "G108"),

    // 사용자 관련 오류 메시지 추가
    USER_NOT_FOUND("해당하는 유저가 존재하지 않습니다.", "G109"),
    INVALID_USER_ID("유효하지 않은 사용자 ID입니다.", "G110"),
    INVALID_PAGING_PARAMETERS("페이징 파라미터가 올바르지 않습니다.", "G111"),

    // 서버 내부 오류
    INTERNAL_SERVER_ERROR("서버 내부 오류입니다.", "G199");

    private final String message;
    private final String code;

    /**
     * 코드로 메시지를 찾는 메서드
     * @param code 찾을 코드
     * @return 해당하는 GuestbookMessageCode enum
     */
    public static GuestbookMessageCode fromCode(String code) {
        for (GuestbookMessageCode messageCode : GuestbookMessageCode.values()) {
            if (messageCode.getCode().equals(code)) {
                return messageCode;
            }
        }
        throw new IllegalArgumentException("Unknown GuestbookMessageCode: " + code);
    }

    /**
     * 성공 메시지인지 확인
     * @return 성공 메시지 여부
     */
    public boolean isSuccess() {
        return this.code.startsWith("G00");
    }

    /**
     * 오류 메시지인지 확인
     * @return 오류 메시지 여부
     */
    public boolean isError() {
        return this.code.startsWith("G10") || this.code.startsWith("G19");
    }
}

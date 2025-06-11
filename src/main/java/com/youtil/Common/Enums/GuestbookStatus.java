package com.youtil.Common.Enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GuestbookStatus {
    ACTIVE("active", "활성"),
    DEACTIVE("deactive", "비활성");

    private final String value;
    private final String description;

    // 방명록 관련 상수들
    public static final int MAX_CONTENT_LENGTH = 50;
    public static final int MIN_CONTENT_LENGTH = 1;
    public static final int MAX_REPLY_DEPTH = 1; // 답글은 1단계까지만

    // 페이징 관련 상수
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // 삭제된 댓글 표시 메시지
    public static final String DELETED_COMMENT_MESSAGE = "삭제된 댓글입니다.";

    /**
     * 문자열 값으로 enum을 찾는 메서드
     * @param value 찾을 문자열 값
     * @return 해당하는 GuestbookStatus enum
     */
    public static GuestbookStatus fromValue(String value) {
        for (GuestbookStatus status : GuestbookStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown GuestbookStatus value: " + value);
    }

    /**
     * 활성 상태인지 확인
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 비활성 상태인지 확인
     * @return 비활성 상태 여부
     */
    public boolean isDeactive() {
        return this == DEACTIVE;
    }

    /**
     * JPA AttributeConverter - DB의 문자열 값과 enum 매핑
     */
    @Converter(autoApply = true)
    public static class GuestbookStatusConverter implements AttributeConverter<GuestbookStatus, String> {

        @Override
        public String convertToDatabaseColumn(GuestbookStatus attribute) {
            if (attribute == null) {
                return null;
            }
            return attribute.getValue();
        }

        @Override
        public GuestbookStatus convertToEntityAttribute(String dbData) {
            if (dbData == null) {
                return null;
            }
            return GuestbookStatus.fromValue(dbData);
        }
    }
}

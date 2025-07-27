package com.youtil.Common.DuplicatePrevention;

import org.springframework.stereotype.Component;

@Component
public class DuplicatePreventionConfig {

    // 액션별 제한 시간 (초)
    public static final int GUESTBOOK_LIMIT_SECONDS = 5;
    public static final int TIL_RECOMMEND_LIMIT_SECONDS = 3;
    public static final int COMMENT_LIMIT_SECONDS = 5;
    public static final int TIL_CREATE_LIMIT_SECONDS = 10;
    public static final int UPDATE_LIMIT_SECONDS = 5;
    public static final int DELETE_LIMIT_SECONDS = 3;
    public static final int TIL_UPLOAD_LIMIT_SECONDS = 10;
    public static final int INTERVIEW_CREATE_LIMIT_SECONDS = 10;
    public static final int DEFAULT_LIMIT_SECONDS = 5;

    // 카프카 관련 설정
    public static final String DUPLICATE_CHECK_TOPIC = "duplicate.check.topic";
    public static final int RESPONSE_TIMEOUT_SECONDS = 10;

    // 메모리 관리 설정
    public static final int MAX_REQUESTS_IN_MEMORY = 50000;
    public static final int CLEANUP_THRESHOLD_MINUTES = 5;

    public static int getLimitSeconds(String action) {
        if (action == null) return DEFAULT_LIMIT_SECONDS;

        return switch (action) {
            case "guestbook", "guestbook_create" -> GUESTBOOK_LIMIT_SECONDS;
            case "guestbook_delete", "til_delete","comment_delete" -> DELETE_LIMIT_SECONDS;
            case "guestbook_update", "comment_update", "til_update"-> UPDATE_LIMIT_SECONDS;
            case "til_recommend", "til_like" -> TIL_RECOMMEND_LIMIT_SECONDS;
            case "comment", "comment_create" -> COMMENT_LIMIT_SECONDS;
            case "til_create" -> TIL_CREATE_LIMIT_SECONDS;
            case "til_upload" -> TIL_UPLOAD_LIMIT_SECONDS;
            case "interview_create" -> INTERVIEW_CREATE_LIMIT_SECONDS;
            default -> DEFAULT_LIMIT_SECONDS;
        };
    }

    public static int getLimitMilliseconds(String action) {
        return getLimitSeconds(action) * 1000;
    }

    public static long getCleanupThresholdMilliseconds() {
        return CLEANUP_THRESHOLD_MINUTES * 60 * 1000L;
    }
}

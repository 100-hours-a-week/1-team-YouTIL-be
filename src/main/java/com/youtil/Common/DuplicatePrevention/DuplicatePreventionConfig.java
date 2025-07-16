package com.youtil.Common.DuplicatePrevention;

import org.springframework.stereotype.Component;

@Component
public class DuplicatePreventionConfig {

    // 액션별 제한 시간 (초)
    public static final int GUESTBOOK_LIMIT_SECONDS = 5;
    public static final int TIL_RECOMMEND_LIMIT_SECONDS = 3;
    public static final int COMMENT_LIMIT_SECONDS = 5;
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
            case "guestbook" -> GUESTBOOK_LIMIT_SECONDS;
            case "til_recommend" -> TIL_RECOMMEND_LIMIT_SECONDS;
            case "comment" -> COMMENT_LIMIT_SECONDS;
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

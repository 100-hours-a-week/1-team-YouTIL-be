package com.youtil.Common.DuplicatePrevention;

import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * 중복 요청 제어 설정 중앙 관리
 */
@Component
@Getter
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
    public static final int MAX_REQUESTS_IN_MEMORY = 10000;
    public static final int CLEANUP_THRESHOLD_MINUTES = 10;

    // 시간 계산용 상수
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int SECONDS_PER_MINUTE = 60;
    public static final int MILLISECONDS_PER_MINUTE = SECONDS_PER_MINUTE * MILLISECONDS_PER_SECOND;

    /**
     * 액션별 제한 시간 반환 (초)
     */
    public static int getLimitSeconds(String action) {
        return switch (action) {
            case "guestbook" -> GUESTBOOK_LIMIT_SECONDS;
            case "til_recommend" -> TIL_RECOMMEND_LIMIT_SECONDS;
            case "comment" -> COMMENT_LIMIT_SECONDS;
            default -> DEFAULT_LIMIT_SECONDS;
        };
    }

    /**
     * 액션별 제한 시간 반환 (밀리초)
     */
    public static int getLimitMilliseconds(String action) {
        return getLimitSeconds(action) * MILLISECONDS_PER_SECOND;
    }

    /**
     * 정리 기준 시간 반환 (밀리초)
     */
    public static long getCleanupThresholdMilliseconds() {
        return CLEANUP_THRESHOLD_MINUTES * MILLISECONDS_PER_MINUTE;
    }
}

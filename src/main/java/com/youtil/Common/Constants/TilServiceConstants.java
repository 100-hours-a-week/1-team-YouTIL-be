package com.youtil.Common.Constants;

import java.time.Duration;
import java.util.UUID;

public class TilServiceConstants {

    //TIL Request Handler 관련
    public static final String STREAM_KEY = "ai:til:stream";
    public static final String GROUP = "ai-group";
    public static final Duration RESULT_TTL = Duration.ofMinutes(5);
    public static final String OWNER_KEY_PREFIX = "til:owner:";
    public static final Duration OWNER_TTL = Duration.ofSeconds(30);

    public static final String CONSUMER = "consumer" + UUID.randomUUID();

    public static final String RETRY_COUNT = "retryCount";
    public static final String RESULT_KEY = "ai:til:result";
    public static final String RESULT_ERROR_VALUE = "{\"error\":\"동시성 초과로 재시도 취소됨\"}";

    public static final String REQUEST_JSON_KEY = "requestJson";
    public static final String REQUEST_ID_KEY = "requestId";
    public static final String USER_ID_KEY = "userId";

    public static final String TIL_WORKER_NAME = "til-worker-";
    public static final int MAX_TIL_WORKER_THREADS = 10;
    public static final int MAX_STREAM_FETCH_COUNT = 5;

    private TilServiceConstants() {
    }
}

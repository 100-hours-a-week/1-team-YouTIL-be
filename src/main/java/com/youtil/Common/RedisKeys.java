package com.youtil.Common;

public class RedisKeys {

    public static final String TIL_STREAM_KEY = "ai:til:stream";
    public static final String TIL_ACTIVE_SET = "ai:til:active_set";

    public static String resultKey(String requestId) {
        return "ai:til:result:" + requestId;
    }
}


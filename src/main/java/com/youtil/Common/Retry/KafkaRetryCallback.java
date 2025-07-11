package com.youtil.Common.Retry;

@FunctionalInterface
public interface KafkaRetryCallback {

    void retry(String requestJson, String requestId);
}

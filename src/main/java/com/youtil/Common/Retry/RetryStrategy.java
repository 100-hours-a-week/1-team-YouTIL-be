package com.youtil.Common.Retry;

import org.springframework.data.redis.connection.stream.MapRecord;

public interface RetryStrategy<Q> {

    boolean shouldRetry(Exception e, int retryCount);

    long nextDelayMillis();

    void retry(MapRecord<String, Object, Object> record, int retryCount);
}

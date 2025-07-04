package com.youtil.Common.Constants;

import java.time.Duration;


public class TilServiceConstants implements AiServiceConstants {

    public TilServiceConstants() {
    }

    //TIL Request Handler 관련
    @Override
    public String getStreamKey() {
        return "ai.til.topic";
    }

    @Override
    public String getGroup() {
        return "til-consumer-group"; // Kafka group.id 설정과 일치해야 함
    }

    @Override
    public String getOwnerKeyPrefix() {
        return "til:owner:";
    }

    @Override
    public Duration getOwnerTtl() {
        return Duration.ofSeconds(30);
    }

    @Override
    public String getResultKey() {
        return "ai:til:result";
    }

    @Override
    public Duration getResultTtl() {
        return Duration.ofMinutes(5);
    }

    @Override
    public String getRequestJsonKey() {
        return "requestJson";
    }


    @Override
    public String getRequestIdKey() {
        return "requestId";
    }


    @Override
    public String getUserIdKey() {
        return "userId";
    }

    @Override
    public String getRetryCountKey() {
        return "retryCount";
    }

    @Override
    public String getConsumerNamePrefix() {
        return "til-consumer-";
    }

    @Override
    public String getWorkerThreadNamePrefix() {
        return "til-worker-";
    }

    @Override
    public int getMaxWorkerThreads() {
        return 10;
    }

    @Override
    public int getMaxStreamFetchCount() {
        return 5;
    }

    @Override
    public int getResendTimeoutSeconds() {
        return 360;
    }

    @Override
    public Duration getSemaphoreTtl() {
        return Duration.ofMinutes(5);
    }

    @Override
    public String getSemaphoreKey() {
        return "ai:til:semaphore";
    }
}

package com.youtil.Common.Constants;

import java.time.Duration;

public class FilteringServiceConstants implements AiServiceConstants {

    @Override
    public String getStreamKey() {
        return "comment.moderation.topic";
    }

    @Override
    public String getGroup() {
        return "comment-moderator";
    }

    @Override
    public String getOwnerKeyPrefix() {
        return "comment:owner:";
    }

    @Override
    public Duration getOwnerTtl() {
        return Duration.ofSeconds(30);
    }

    @Override
    public String getResultKey() {
        return "ai:comment:result";
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
        return "comment-consumer-";
    }

    @Override
    public String getWorkerThreadNamePrefix() {
        return "comment-worker-";
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
        return "ai:comment:semaphore";
    }
}

package com.youtil.Common.Constants;

import java.time.Duration;


public interface AiServiceConstants {

    String getStreamKey();

    String getGroup();

    String getOwnerKeyPrefix();

    Duration getOwnerTtl();

    String getResultKey();

    Duration getResultTtl();

    String getRequestJsonKey();

    String getRequestIdKey();

    String getUserIdKey();

    String getRetryCountKey();

    String getConsumerNamePrefix();

    String getWorkerThreadNamePrefix();

    int getMaxWorkerThreads();

    int getMaxStreamFetchCount();

    int getResendTimeoutSeconds();

    Duration getSemaphoreTtl();

    String getSemaphoreKey();
}

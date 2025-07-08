package com.youtil.Concurrency.policy;

import com.youtil.Common.Enums.AiType;

public interface SemaphorePolicy {

    int getFixedLimit(AiType aiType);

    int getSharedLimit();
}

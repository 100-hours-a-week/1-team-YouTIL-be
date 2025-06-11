package com.youtil.Concurrency.policy;

import com.youtil.Common.Enums.AiType;
import org.springframework.stereotype.Component;

@Component
public class CpuTimeSemaphorePolicy implements SemaphorePolicy {

    private static final int TIL_CPU_SEMAPHORE_COUNT = 1;
    private static final int INTERVIEW_CPU_SEMAPHORE_COUNT = 1;
    private static final int SHARED_CPU_SEMAPHORE_COUNT = 1;

    @Override
    public int getFixedLimit(AiType aiType) {
        return switch (aiType) {
            case TIL -> TIL_CPU_SEMAPHORE_COUNT;
            case INTERVIEW -> INTERVIEW_CPU_SEMAPHORE_COUNT;
        };
    }

    @Override
    public int getSharedLimit() {
        return SHARED_CPU_SEMAPHORE_COUNT;
    }
}

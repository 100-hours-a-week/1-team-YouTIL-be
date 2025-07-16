package com.youtil.Concurrency.policy;

import com.youtil.Common.Enums.AiType;
import org.springframework.stereotype.Component;

@Component
public class GpuTimeSemaphorePolicy implements SemaphorePolicy {

    private static final int TIL_GPU_SEMAPHORE_COUNT = 2;
    private static final int INTERVIEW_GPU_SEMAPHORE_COUNT = 3;
    private static final int SHARED_GPU_SEMAPHORE_COUNT = 7;

    @Override
    public int getFixedLimit(AiType aiType) {
        return switch (aiType) {
            case TIL -> TIL_GPU_SEMAPHORE_COUNT;
            case INTERVIEW -> INTERVIEW_GPU_SEMAPHORE_COUNT;
        };
    }

    @Override
    public int getSharedLimit() {
        return SHARED_GPU_SEMAPHORE_COUNT;
    }
}

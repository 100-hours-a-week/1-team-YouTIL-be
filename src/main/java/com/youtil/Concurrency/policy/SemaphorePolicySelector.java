package com.youtil.Concurrency.policy;

import com.youtil.Concurrency.GpuTimeChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SemaphorePolicySelector {

    private final GpuTimeSemaphorePolicy gpuTimeSemaphorePolicy;
    private final CpuTimeSemaphorePolicy cpuTimeSemaphorePolicy;

    public SemaphorePolicy getSemaphorePolicy() {
        return GpuTimeChecker.isGpuTimeNow() ? gpuTimeSemaphorePolicy : cpuTimeSemaphorePolicy;
    }
}

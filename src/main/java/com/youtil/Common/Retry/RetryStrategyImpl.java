package com.youtil.Common.Retry;

import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Dto.QueueRequest;
import com.youtil.Concurrency.RedisSemaphoreManager;
import com.youtil.Concurrency.RedisSemaphoreManager.SemaphoreAcquireResult;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;

@Slf4j
@RequiredArgsConstructor
public class RetryStrategyImpl<T extends QueueRequest> implements RetryStrategy<String> {

    // 1초마다 실행
    private static final long RETRY_INTERVAL_MS = 1_000;
    // 40분 = 40 * 60 = 2400초 = 2400회
    private static final int MAX_RETRY = 2400;
    private final ScheduledExecutorService scheduler;
    private final AiServiceConstants constants;
    private final RedisSemaphoreManager semaphoreManager;
    private final PriorityBlockingQueue<T> queue;
    private final String aiType;
    private final TriConsumer<String, Long, String> retryAction; // requestJson, requestId

    @Override
    public void retry(String requestJson, Long userId, String requestId, int retryCount,
            Consumer<String> onFail) {
        if (retryCount > MAX_RETRY) {
            log.warn("재시도 초과 - requestId={}", requestId);
            onFail.accept(requestId); // 여기서 실패 처리 위임
            return;
        }

        scheduler.schedule(() -> {
            SemaphoreAcquireResult acquired = semaphoreManager.tryAcquireSemaphore(requestId,
                    aiType, queue);
            if (!acquired.acquired()) {
                retry(requestJson, userId, requestId, retryCount + 1, onFail);
                return;
            }

            try {
                retryAction.accept(requestJson, userId, requestId);
            } catch (Exception e) {
                retry(requestJson, userId, requestId, retryCount + 1, onFail);
            } finally {
                semaphoreManager.releaseSemaphore(requestId, aiType);
            }
        }, RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }


    private void handleFail(String requestId) {
        log.warn("기본 재시도 실패 처리 - requestId={}", requestId);
    }
}

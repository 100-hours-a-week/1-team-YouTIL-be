package com.youtil.Common.Retry;

import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Concurrency.RedisSemaphoreManager;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RequiredArgsConstructor
public class RetryStrategyImpl<Q> implements RetryStrategy<Q> {

    private final ScheduledExecutorService scheduler;
    private final PriorityBlockingQueue<Q> queue;
    private final AiServiceConstants constants;
    private final RedisSemaphoreManager semaphoreManager;
    private final String aiType;
    private final Function<MapRecord<String, Object, Object>, Q> wrapFunction;

    @Override
    public boolean shouldRetry(Exception e, int retryCount) {
        return retryCount < 1 && (e instanceof WebClientRequestException
                || e instanceof WebClientResponseException);
    }

    @Override
    public long nextDelayMillis() {
        return 1500 + ThreadLocalRandom.current().nextInt(500);
    }

    @Override
    public void retry(MapRecord<String, Object, Object> record, int retryCount) {
        String requestId = (String) record.getValue().get(constants.getRequestIdKey());

        if (!semaphoreManager.tryAcquireSemaphore(requestId, aiType)) {
            log.info("재시도 직전 동시성 초과로 재시도 취소: {}", requestId);
            return;
        }

        scheduler.schedule(() -> queue.offer(wrapFunction.apply(record)), nextDelayMillis(),
                TimeUnit.MILLISECONDS);
    }
}

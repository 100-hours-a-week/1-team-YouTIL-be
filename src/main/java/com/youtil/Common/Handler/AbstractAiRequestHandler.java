package com.youtil.Common.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.AiProgress;
import com.youtil.Common.Retry.RetryStrategy;
import com.youtil.Common.Sse.SseEmitterService;
import com.youtil.Concurrency.RedisSemaphoreManager;
import com.youtil.Concurrency.RedisSemaphoreManager.SemaphoreAcquireResult;
import java.net.ConnectException;
import java.util.concurrent.ScheduledExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAiRequestHandler<T> {

    protected final StringRedisTemplate redisTemplate;
    protected final ObjectMapper objectMapper;
    protected final RedisSemaphoreManager semaphoreManager;
    protected final ScheduledExecutorService scheduler;
    protected final AiServiceConstants constants;
    protected final RetryStrategy<String> retryStrategy;
    protected final SseEmitterService sseEmitterService;

    public void asyncProcess(String requestJson, Long userId, String requestId) {
        scheduler.submit(() -> process(requestJson, userId, requestId));
    }

    public void process(String requestJson, Long userId, String requestId) {
        SemaphoreAcquireResult result = tryAcquire(requestId);
        try {
            if (!result.acquired()) {
                log.warn("세마포어 획득 실패 - requestId={}", requestId);
                retryStrategy.retry(requestJson, userId, requestId, 0,
                        this::setErrorResult); // 즉시 재시도 등록
                return;
            }
            sseEmitterService.send(requestId, AiProgress.PROCESSING, 0, 0);
            // AI 요청 및 응답 저장
            T response = handleRequest(requestJson, userId, requestId);

            redisTemplate.opsForValue().set(
                    constants.getResultKey() + requestId,
                    objectMapper.writeValueAsString(response),
                    constants.getResultTtl()
            );

            logSuccess(requestId);

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 실패 - requestId={}, error={}", requestId, e.getMessage(), e);
            setErrorResult(requestId);
//            if (isNonRetryableException(e)) {
//                 // 종료
//
//            }
//            retryStrategy.retry(requestJson, userId, requestId, 1,
//                    this::setErrorResult); // 예외 발생 시 재시도
        } finally {
            if (result.acquired()) {
                semaphoreManager.releaseSemaphore(requestId, getAiType());
            }
        }
    }


    public void setErrorResult(String requestId) {
        try {
            sseEmitterService.send(requestId, AiProgress.ERROR, 0, 0);

        } catch (Exception e) {
            log.error("에러 결과 저장 실패 - {}", e.getMessage(), e);
        }
    }

    public void handleRequestProcess(String requestJson, Long userId, String requestId) {
        try {
            sseEmitterService.send(requestId, AiProgress.PROCESSING, 0, 0);
            T response = handleRequest(requestJson, userId, requestId);

            redisTemplate.opsForValue().set(
                    constants.getResultKey() + requestId,
                    objectMapper.writeValueAsString(response),
                    constants.getResultTtl()
            );

            logSuccess(requestId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isNonRetryableException(Throwable e) {
        if (e instanceof WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status == 422 || status == 403;
        }
        if (e instanceof WebClientRequestException ex
                && ex.getCause() instanceof ConnectException) {
            return true;
        }
        return false;
    }

    protected abstract String getAiType();


    protected abstract T handleRequest(String requestJson, long userId,
            String requestId) throws Exception;

    protected abstract void logSuccess(String requestId);

    protected abstract Object getEmptyErrorResponse();

    protected abstract SemaphoreAcquireResult tryAcquire(String requestId);
}

package com.youtil.Common.Handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Concurrency.RedisSemaphoreManager;
import io.jsonwebtoken.io.SerializationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAiRequestHandler<T, Q> {

    protected final StringRedisTemplate redisTemplate;
    protected final ObjectMapper objectMapper;
    protected final RedisSemaphoreManager semaphoreManager;
    @Qualifier("delayScheduler")
    protected final ScheduledExecutorService scheduler;
    protected final PriorityBlockingQueue<Q> processingQueue;
    protected final AiServiceConstants constants;

    protected AbstractAiRequestHandler(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisSemaphoreManager semaphoreManager,
            PriorityBlockingQueue<Q> processingQueue,
            AiServiceConstants constants,
            ScheduledExecutorService scheduler) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.semaphoreManager = semaphoreManager;
        this.processingQueue = processingQueue;
        this.constants = constants;
        this.scheduler = scheduler;
    }

    public void process(MapRecord<String, Object, Object> record) {
        Map<Object, Object> data = record.getValue();
        String requestId = (String) data.get(constants.getRequestIdKey());
        String userId = (String) data.get(constants.getUserIdKey());
        String requestJson = (String) data.get(constants.getRequestJsonKey());

        if (!tryAcquireOwnership(requestId)) {
            requeueWithDelay(record);
            return;
        }

        try {
            if (!semaphoreManager.tryAcquireSemaphore(requestId, getAiType())) {
                releaseOwnership(requestId);
                requeueWithDelay(record);
                return;
            }

            T response = handleRequest(requestJson, Long.parseLong(userId));

            redisTemplate.opsForValue().set(
                    constants.getResultKey() + requestId,
                    objectMapper.writeValueAsString(response),
                    constants.getResultTtl());

            acknowledgeAndDelete(record);
            logSuccess(requestId);

        } catch (Exception e) {
            log.error("AI 처리 실패 - requestId={}, error={}", requestId, e.getMessage(), e);
            handleRetry(record, data, requestId, e);

        } finally {
            releaseOwnership(requestId);
            semaphoreManager.releaseSemaphore(requestId, getAiType());
        }
    }

    private boolean tryAcquireOwnership(String requestId) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(constants.getOwnerKeyPrefix() + requestId,
                        Thread.currentThread().getName(),
                        constants.getOwnerTtl()));
    }

    private void releaseOwnership(String requestId) {
        redisTemplate.delete(constants.getOwnerKeyPrefix() + requestId);
    }

    private void acknowledgeAndDelete(MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream()
                .acknowledge(constants.getStreamKey(), constants.getGroup(), record.getId());
        redisTemplate.opsForStream().delete(constants.getStreamKey(), record.getId());
    }

    private boolean isRetryableException(Throwable e) {
        return hasCause(e, WebClientRequestException.class) || hasCause(e,
                WebClientResponseException.class);
    }

    private boolean hasCause(Throwable e, Class<? extends Throwable> clazz) {
        while (e != null) {
            if (clazz.isInstance(e)) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    private void requeueWithDelay(MapRecord<String, Object, Object> record) {
        try {
            Thread.sleep(1000 + ThreadLocalRandom.current().nextInt(500));
        } catch (InterruptedException ignored) {
        }
        processingQueue.offer(wrap(record));
    }

    private void handleRetry(MapRecord<String, Object, Object> record, Map<Object, Object> data,
            String requestId, Exception e) {
        int retryCount = Integer.parseInt(
                String.valueOf(data.getOrDefault(constants.getRetryCountKey(), "0")));
        if (retryCount < 1 && isRetryableException(e)) {
            Map<Object, Object> newData = new HashMap<>(data);
            newData.put(constants.getRetryCountKey(), retryCount + 1);
            MapRecord<String, Object, Object> retryRecord = MapRecord.create(record.getStream(),
                    newData).withId(record.getId());

            if (semaphoreManager.tryAcquireSemaphore(requestId, getAiType())) {
                scheduler.schedule(() -> processingQueue.offer(wrap(retryRecord)),
                        1500 + ThreadLocalRandom.current().nextInt(500),
                        TimeUnit.MILLISECONDS);
            } else {
                log.info("재시도 직전 동시성 초과로 재시도 취소: {}", requestId);
                setErrorResult(requestId);
            }
        } else {
            setErrorResult(requestId);
        }

        acknowledgeAndDelete(record);
    }

    protected void setErrorResult(String requestId) {

        try {
            redisTemplate.opsForValue().set(
                    constants.getResultKey() + requestId,
                    objectMapper.writeValueAsString(getEmptyErrorResponse()),
                    constants.getResultTtl());
        } catch (JsonProcessingException e) {
            log.error("에러 응답 저장 실패", e);
        } catch (RedisConnectionFailureException e) {
            log.error("레디스 접속 에러", e);
        } catch (SerializationException e) {
            log.error("직력화 실패", e);

        } catch (IllegalArgumentException e) {
            log.error("부적절한 값 포함", e);
        } catch (Exception e) {
            log.error("알수없는 예외가 발생했습니다.", e);
        }
    }

    protected abstract String getAiType();

    protected abstract T handleRequest(String requestJson, long userId) throws Exception;

    protected abstract void logSuccess(String requestId);

    protected abstract Object getEmptyErrorResponse();

    protected abstract Q wrap(MapRecord<String, Object, Object> record);
}

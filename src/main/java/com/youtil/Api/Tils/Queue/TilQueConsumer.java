package com.youtil.Api.Tils.Queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.redis.serializer.SerializationException;
import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Api.Tils.Handler.TilRequestHandler;
import static com.youtil.Common.Constants.TilServiceConstants.CONSUMER;
import static com.youtil.Common.Constants.TilServiceConstants.CONSUMER_THREAD_NAME;
import static com.youtil.Common.Constants.TilServiceConstants.GROUP;
import static com.youtil.Common.Constants.TilServiceConstants.MAX_STREAM_FETCH_COUNT;
import static com.youtil.Common.Constants.TilServiceConstants.MAX_TIL_WORKER_THREADS;
import static com.youtil.Common.Constants.TilServiceConstants.STREAM_KEY;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TilQueConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final TilRequestHandler tilRequestHandler;
    private final PriorityBlockingQueue<PrioritizedTilRequest> processingQueue;
    private final ExecutorService tilWorkerThreadPool;

    private volatile boolean running = true;
    private Thread consumerThread;

    @PostConstruct
    public void init() {
        initGroup();
        startConsumerThread();
        initWorkers();
    }

    private void initGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
            log.info("레디스 스트림 그룹 '{}' 생성됨", GROUP);
        } catch (RedisSystemException e) {
            log.warn("레디스 그룹 생성 중 시스템 예외 발생: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("그룹 생성 파라미터 문제: {}", e.getMessage());
        } catch (Exception e) {
            log.error("그룹 생성 중 알 수 없는 예외 발생", e);
        }
    }

    private void initWorkers() {
        for (int i = 0; i < MAX_TIL_WORKER_THREADS; i++) {
            tilWorkerThreadPool.submit(() -> {
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        MapRecord<String, Object, Object> record = processingQueue.take().getRecord();
                        tilRequestHandler.process(record);
                    }catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (RedisSystemException e) {
                        log.error("Redis 통신 오류", e);
                    } catch (IllegalStateException e) {
                        log.error("애플리케이션 상태 오류", e);
                        break; // 컨슈머 중단 고려해서 break
                    } catch (SerializationException e) {
                        log.warn("Serialization 실패, 작업 건너뜀", e);
                    } catch (NullPointerException e) {
                        log.error("데이터 무결성 문제 발생", e);
                    } catch (RejectedExecutionException e) {
                        log.warn("작업 제출 거부 - 스레드 풀 포화", e);
                    } catch (Exception e) {
                        log.error("워크 처리 중 알 수 없는 예외", e);
                    }
                }
            });
        }
    }

    private void startConsumerThread() {
        for (int i = 0; i < MAX_TIL_WORKER_THREADS; i++) {
            final int consumerIndex = i;
            Thread consumerThread = new Thread(() -> {
                String consumerId = CONSUMER+ consumerIndex;

                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        consume(consumerId);
                    } catch (RedisSystemException e) {
                        log.error("Redis 연결/통신 문제 발생", e);
                        backoff(1000);
                    } catch (IllegalArgumentException e) {
                        log.error("소비자 초기화 파라미터 문제 발생", e);
                        break; // 계속 시도해도 의미 없으므로 종료
                    } catch (Exception e) {
                        log.error("Redis Consume 중 알 수 없는 예외", e);
                        backoff(1000);
                    }
                }
            }, CONSUMER_THREAD_NAME + "-" + i);

            consumerThread.setDaemon(true);
            consumerThread.start();
        }
}

    public void consume(String consumerId) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(GROUP, consumerId),
                StreamReadOptions.empty()
                        .block(Duration.ofSeconds(5))
                        .count(MAX_STREAM_FETCH_COUNT),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
        );

        if (records != null) {
            for (MapRecord<String, Object, Object> record : records) {
                processingQueue.offer(new PrioritizedTilRequest(record));
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("TilQueConsumer 종료 중...");
        running = false;

        if (consumerThread != null) {
            consumerThread.interrupt();
        }

        tilWorkerThreadPool.shutdownNow();
        log.info("TilQueConsumer 종료 완료");
    }

    private void backoff(long millis) {
        for (long slept = 0; slept < millis && running; slept += 100) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

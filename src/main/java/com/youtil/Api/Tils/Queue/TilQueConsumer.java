package com.youtil.Api.Tils.Queue;

import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Api.Tils.Handler.TilRequestHandler;
import com.youtil.Common.Constants.AiServiceConstants;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TilQueConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final TilRequestHandler tilRequestHandler;
    private final PriorityBlockingQueue<PrioritizedTilRequest> processingQueue;
    private final ExecutorService tilWorkerThreadPool;
    private final List<Thread> consumerThreads = new CopyOnWriteArrayList<>();
    @Qualifier("tilServiceConstants")
    private final AiServiceConstants tilServiceConstants;
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
            stringRedisTemplate.opsForStream().createGroup(tilServiceConstants.getStreamKey(),
                    tilServiceConstants.getGroup());
            log.info("레디스 스트림 그룹 '{}' 생성됨", tilServiceConstants.getGroup());
        } catch (RedisSystemException e) {
            log.warn("레디스 그룹 생성 중 시스템 예외 발생: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("그룹 생성 파라미터 문제: {}", e.getMessage());
        } catch (Exception e) {
            log.error("그룹 생성 중 알 수 없는 예외 발생", e);
        }
    }

    private void initWorkers() {
        for (int i = 0; i < tilServiceConstants.getMaxWorkerThreads(); i++) {
            tilWorkerThreadPool.submit(() -> {
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        MapRecord<String, Object, Object> record = processingQueue.take()
                                .getRecord();
                        tilRequestHandler.process(record);
                    } catch (InterruptedException e) {
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
        for (int i = 0; i < tilServiceConstants.getMaxWorkerThreads(); i++) {
            final int consumerIndex = i;
            Thread consumerThread = new Thread(() -> {
                String consumerId = tilServiceConstants.getConsumerNamePrefix() + consumerIndex;

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
            }, tilServiceConstants.getWorkerThreadNamePrefix() + "-" + i);

            consumerThread.setDaemon(true);
            consumerThread.start();
            consumerThreads.add(consumerThread);
        }
    }

    public void consume(String consumerId) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(tilServiceConstants.getGroup(), consumerId),
                StreamReadOptions.empty()
                        .block(Duration.ofSeconds(5))
                        .count(tilServiceConstants.getMaxStreamFetchCount()),
                StreamOffset.create(tilServiceConstants.getStreamKey(), ReadOffset.lastConsumed())
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

        // 모든 consumer 스레드 종료 대기
        for (Thread thread : consumerThreads) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                try {
                    thread.join(2000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 워커 스레드 종료 대기
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

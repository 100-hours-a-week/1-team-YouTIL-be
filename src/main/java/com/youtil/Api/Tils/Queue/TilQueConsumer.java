package com.youtil.Api.Tils.Queue;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        } catch (Exception e) {
            log.warn("레디스 스트림 그룹 '{}' 이미 존재하거나 초기화되지 않음", GROUP);
        }
    }

    private void initWorkers() {
        for (int i = 0; i < MAX_TIL_WORKER_THREADS; i++) {
            tilWorkerThreadPool.submit(() -> {
                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        MapRecord<String, Object, Object> record = processingQueue.take()
                                .getRecord();
                        tilRequestHandler.process(record);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.warn("워크 처리 중 예외 발생", e);
                    }
                }
            });
        }
    }

    private void startConsumerThread() {
        Thread thread = new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    consume();
                } catch (Exception e) {
                    log.error("Redis Consume 중 에러 발생", e);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, CONSUMER_THREAD_NAME);

        thread.setDaemon(true);
        thread.start();
        this.consumerThread = thread;
    }

    public void consume() {

        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(GROUP, CONSUMER),
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
}

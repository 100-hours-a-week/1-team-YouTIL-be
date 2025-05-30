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

@Component
@RequiredArgsConstructor
@Slf4j
public class TilQueConsumer {


    private final StringRedisTemplate stringRedisTemplate;

    private final TilRequestHandler tilRequestHandler;
    private final PriorityBlockingQueue<PrioritizedTilRequest> processingQueue;
    private final ExecutorService tilWorkerThreadPool;

    @PostConstruct
    public void startConsumerThread() {
        Thread consumerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    consume(); // 블로킹 방식으로 데이터 들어올 때만 처리
                } catch (Exception e) {
                    log.error("Redis Consume 중 에러 발생", e);
                    try {
                        Thread.sleep(1000); // 에러 발생 시 잠시 대기 후 재시도
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, CONSUMER_THREAD_NAME);

        consumerThread.setDaemon(true); // 서버 종료시 같이 종료되도록 설정 (선택)
        consumerThread.start();
    }

    @PostConstruct
    public void initGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
            log.info("레디스 스트림 그룹  '{}' 생성됨", GROUP);
        } catch (Exception e) {
            log.warn("레디스 스트림 그룹 '{}' 이미 존재하거나, 초기화가 되지 않았음", GROUP);
        }
    }

    @PostConstruct
    public void initWorkers() {
        for (int i = 0; i < MAX_TIL_WORKER_THREADS; i++) {
            tilWorkerThreadPool.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
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

    public void consume() {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(GROUP, CONSUMER),
                StreamReadOptions.empty()
                        .block(Duration.ofSeconds(5)) // 최대 5초 대기
                        .count(MAX_STREAM_FETCH_COUNT),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
        );

        if (records != null) {
            for (MapRecord<String, Object, Object> record : records) {
                processingQueue.offer(new PrioritizedTilRequest(record));
            }
        }
    }

}

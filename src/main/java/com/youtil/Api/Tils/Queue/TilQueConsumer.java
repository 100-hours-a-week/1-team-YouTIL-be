package com.youtil.Api.Tils.Queue;

import com.youtil.Api.Tils.Handler.TilRequestHandler;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TilQueConsumer {

    private static final String STREAM_KEY = "ai:til:stream";
    private static final String GROUP = "ai-group";
    private static final String CONSUMER = "consumer" + UUID.randomUUID();
    private final StringRedisTemplate stringRedisTemplate;

    private final TilRequestHandler tilRequestHandler;
    private final BlockingQueue<MapRecord<String, Object, Object>> processingQueue;

    @PostConstruct
    public void initGroup() {
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP);
            log.info("Redis Stream Group '{}' created", GROUP);
        } catch (Exception e) {
            log.warn("Redis Stream Group '{}' already exists or stream not initialized", GROUP);
        }
    }

    @PostConstruct
    public void initWorkers() {
        for (int i = 0; i < 10; i++) {  // 제한 인원만큼만 쓰레드 생성
            new Thread(() -> {
                while (true) {
                    try {
                        MapRecord<String, Object, Object> record = processingQueue.take();
                        tilRequestHandler.process(record);  // sync
                    } catch (Exception e) {
                        log.error("Queue processing error", e);
                    }
                }
            }, "til-worker-" + i).start();
        }
    }

    @Scheduled(fixedDelay = 500)
    public void consume() {
        // 스트림에서 읽어온 레코드들을 BlockingQueue에 넣기만 함
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(GROUP, CONSUMER),
                StreamReadOptions.empty().count(5),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
        );
        if (records != null) {
            processingQueue.addAll(records);
        }
    }

}

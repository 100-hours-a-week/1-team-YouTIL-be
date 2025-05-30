package com.youtil.Api.Tils.Queue;

import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Api.Tils.Handler.TilRequestHandler;
import static com.youtil.Common.Constants.TilServiceConstants.CONSUMER;
import static com.youtil.Common.Constants.TilServiceConstants.GROUP;
import static com.youtil.Common.Constants.TilServiceConstants.MAX_STREAM_FETCH_COUNT;
import static com.youtil.Common.Constants.TilServiceConstants.MAX_TIL_WORKER_THREADS;
import static com.youtil.Common.Constants.TilServiceConstants.STREAM_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.TIL_WORKER_NAME;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
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


    private final StringRedisTemplate stringRedisTemplate;

    private final TilRequestHandler tilRequestHandler;
    private final PriorityBlockingQueue<PrioritizedTilRequest> processingQueue;

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
            new Thread(() -> {
                while (true) {
                    try {
                        MapRecord<String, Object, Object> record = processingQueue.take()
                                .getRecord();
                        tilRequestHandler.process(record);
                    } catch (Exception e) {
                        //throw로 예외를 던지지 않고, while문안에서 워커들이 반복적으로 생성하도록 설정
                        log.warn("큐 삽입에 문제가 있습니다.", e);
                    }
                }
            }, TIL_WORKER_NAME + i).start();
        }
    }

    @Scheduled(fixedDelay = 500)
    public void consume() {
        // 스트림에서 읽어온 레코드들을 BlockingQueue에 넣기만 함
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(GROUP, CONSUMER),
                StreamReadOptions.empty().count(MAX_STREAM_FETCH_COUNT),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
        );
        if (records != null) {
            for (MapRecord<String, Object, Object> record : records) {
                processingQueue.offer(new PrioritizedTilRequest(record));
            }
        }
    }

}

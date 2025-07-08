package com.youtil.Api.Tils.Queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Api.Tils.Dto.TilResponseDTO.TilStatus;
import com.youtil.Api.Tils.Handler.TilRequestHandler;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Sse.SseEmitterService;
import com.youtil.Concurrency.RedisSemaphoreManager;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TilQueConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final TilRequestHandler tilRequestHandler;
    private final PriorityBlockingQueue<PrioritizedTilRequest> processingQueue;
    private final ExecutorService executorService;
    private final AiServiceConstants tilServiceConstants;
    private final ObjectMapper objectMapper;
    private final RedisSemaphoreManager semaphoreManager;
    private final SseEmitterService sseEmitterService;

    public TilQueConsumer(
            StringRedisTemplate stringRedisTemplate,
            TilRequestHandler tilRequestHandler,
            PriorityBlockingQueue<PrioritizedTilRequest> processingQueue,
            @Qualifier("tilWorkerThreadPool") ExecutorService executorService,
            @Qualifier("tilServiceConstants") AiServiceConstants tilServiceConstants,
            ObjectMapper objectMapper,
            RedisSemaphoreManager semaphoreManager,
            SseEmitterService sseEmitterService
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.tilRequestHandler = tilRequestHandler;
        this.processingQueue = processingQueue;
        this.executorService = executorService;
        this.tilServiceConstants = tilServiceConstants;
        this.objectMapper = objectMapper;
        this.semaphoreManager = semaphoreManager;
        this.sseEmitterService = sseEmitterService;
    }


    @KafkaListener(
            topics = "${spring.kafka.consumers.til.request.topic}",
            groupId = "${spring.kafka.consumers.til.request.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void tilRequestConsume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String message = record.value();
        String requestId = record.key();
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            String requestJson = (String) payload.get(tilServiceConstants.getRequestJsonKey());
            Long userId = Long.parseLong((String) payload.get(tilServiceConstants.getUserIdKey()));
            Object enqueueTimeRaw = payload.get("enqueueTime");
            Long timestamp = Long.parseLong((String) enqueueTimeRaw);

            PrioritizedTilRequest prioritizedRequest = new PrioritizedTilRequest(
                    requestJson, userId, requestId, timestamp, ack
            );

            processingQueue.put(prioritizedRequest);
            log.info("큐 삽입 - requestId={}, enqueueTime={}", requestId, timestamp);

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 예외 발생 - requestId={}, message={}", requestId, message, e);
        }
    }

    @KafkaListener(
            topics = "${spring.kafka.consumers.til.process.topic}",
            groupId = "${spring.kafka.consumers.til.process.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void tilProcessConsume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String message = record.value();
        String requestId = record.key();

        try {

            TilStatus tilStatus = TilStatus.builder()
                    .requestId(requestId)
                    .status(message)
                    .total(0L)
                    .position(0)
                    .build();
            log.info(requestId + " " + message);
            sseEmitterService.send(requestId, tilStatus);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 예외 발생 - requestId={}, message={}", requestId, message, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("TilQueConsumer 종료 중...");
        executorService.shutdownNow();
        log.info("TilQueConsumer 종료 완료");
    }


}

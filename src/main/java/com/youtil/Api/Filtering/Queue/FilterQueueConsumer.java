package com.youtil.Api.Filtering.Queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Filtering.Dto.PrioritizedFilterReqeust;
import com.youtil.Common.Constants.AiServiceConstants;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilterQueueConsumer {

    private final PriorityBlockingQueue<PrioritizedFilterReqeust> processingQueue;
    private final List<Thread> consumerThreads = new CopyOnWriteArrayList<>();
    @Qualifier("filterServiceConstants")
    private final AiServiceConstants filterServiceConstants;
    private final ObjectMapper objectMapper;
    @Qualifier("filterWorkerThreadPool")
    ExecutorService executorService;
    private volatile boolean running = true;

    @KafkaListener(
            topics = "${spring.kafka.consumers.comment.filter.topic}",
            groupId = "${spring.kafka.consumers.comment.filter.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String message = record.value();
        String requestId = record.key();

        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            String requestJson = (String) payload.get(
                    filterServiceConstants.getRequestJsonKey());
            Long userId = Long.parseLong(
                    (String) payload.get(filterServiceConstants.getUserIdKey()));
            Object enqueueTimeRaw = payload.get("enqueueTime");
            Long timestamp = Long.parseLong((String) enqueueTimeRaw);

            PrioritizedFilterReqeust prioritizedRequest = new PrioritizedFilterReqeust(
                    requestJson, userId, requestId, timestamp, ack
            );

            processingQueue.put(prioritizedRequest);
            log.info("큐 삽입 - requestId={}, enqueueTime={}", requestId, timestamp);

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 예외 발생 - requestId={}, message={}", requestId, message, e);
        }

    }

    @PreDestroy
    public void shutdown() {
        log.info("InterviewQueConsumer 종료 중...");
        executorService.shutdownNow();
        log.info("InterviewQueConsumer 종료 완료");
    }

}

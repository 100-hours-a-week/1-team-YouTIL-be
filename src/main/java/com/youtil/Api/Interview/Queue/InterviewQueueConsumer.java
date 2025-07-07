package com.youtil.Api.Interview.Queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Interview.Handler.InterviewRequestHandler;
import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewQueueConsumer {

    private final StringRedisTemplate stringRedisTemplate;
    private final InterviewRequestHandler interviewRequestHandler;
    private final PriorityBlockingQueue<PrioritizedInterviewRequest> processingQueue;
    private final List<Thread> consumerThreads = new CopyOnWriteArrayList<>();
    @Qualifier("interviewServiceConstants")
    private final AiServiceConstants interviewServiceConstants;
    private final ObjectMapper objectMapper;
    @Qualifier("interviewWorkerThreadPool")
    ExecutorService executorService;
    private volatile boolean running = true;

    @KafkaListener(
            topics = "${spring.kafka.consumers.interview.request.topic}",
            groupId = "${spring.kafka.consumers.interview.request.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        String message = record.value();
        String requestId = record.key();

        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            String requestJson = (String) payload.get(
                    interviewServiceConstants.getRequestJsonKey());
            Long userId = Long.parseLong(
                    (String) payload.get(interviewServiceConstants.getUserIdKey()));
            Object enqueueTimeRaw = payload.get("enqueueTime");
            Long timestamp = Long.parseLong((String) enqueueTimeRaw);

            PrioritizedInterviewRequest prioritizedRequest = new PrioritizedInterviewRequest(
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

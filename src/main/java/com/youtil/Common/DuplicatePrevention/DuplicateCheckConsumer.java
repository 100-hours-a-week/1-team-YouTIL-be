package com.youtil.Common.DuplicatePrevention;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Common.Dto.DuplicateCheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicateCheckConsumer {

    private final ObjectMapper objectMapper;
    private final DuplicatePreventionManager duplicatePreventionManager;

    // 사용자별 최근 요청 기록 (userId:action:dataHash -> 마지막 요청 시간)
    private final ConcurrentHashMap<String, Long> recentRequests = new ConcurrentHashMap<>();

    /**
     * 설정된 토픽에서 중복 체크 요청 처리
     */
    @KafkaListener(
            topics = "${spring.kafka.consumers.duplicate.check.topic}",
            groupId = "${spring.kafka.consumers.duplicate.check.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDuplicateCheckRequest(ConsumerRecord<String, String> record) {
        try {
            String requestId = record.key();
            String message = record.value();

            DuplicateCheckRequest request = objectMapper.readValue(message, DuplicateCheckRequest.class);

            log.info("중복 방지 요청 수신 - action: {}, userId: {}, requestId: {}",
                    request.getAction(), request.getUserId(), requestId);

            // 중복 체크 로직 수행
            boolean isAllowed = checkDuplicate(request);

            duplicatePreventionManager.handleResponse(requestId, isAllowed);

            log.info("중복 방지 처리 완료 - requestId: {}, allowed: {}", requestId, isAllowed);

        } catch (Exception e) {
            log.error("중복 방지 요청 처리 중 오류: {}", e.getMessage(), e);
        }
    }

    /**
     * 실제 중복 체크 로직
     */
    private boolean checkDuplicate(DuplicateCheckRequest request) {
        String key = String.format("%d:%s:%s",
                request.getUserId(), request.getAction(), request.getDataHash());

        long currentTime = System.currentTimeMillis();
        Long lastRequestTime = recentRequests.get(key);

        // 설정된 제한 시간 계산 (밀리초)
        int limitMilliseconds = DuplicatePreventionConfig.getLimitMilliseconds(request.getAction());

        if (lastRequestTime != null && (currentTime - lastRequestTime) < limitMilliseconds) {
            // 중복 요청으로 판단
            log.warn("중복 요청 감지 - key: {}, 이전요청: {}ms 전",
                    key, currentTime - lastRequestTime);
            return false;
        }

        // 새로운 요청이므로 기록하고 허용
        recentRequests.put(key, currentTime);

        // 설정된 최대 개수 초과 시 오래된 기록 정리
        if (recentRequests.size() > DuplicatePreventionConfig.MAX_REQUESTS_IN_MEMORY) {
            cleanupOldRequests(currentTime);
        }

        return true;
    }

    /**
     * 액션별 제한 시간 반환 (초)
     */
    private int getLimitSeconds(String action) {
        return DuplicatePreventionConfig.getLimitSeconds(action);
    }

    /**
     * 설정된 시간 이상 된 오래된 요청 기록 정리
     */
    private void cleanupOldRequests(long currentTime) {
        long thresholdTime = currentTime - DuplicatePreventionConfig.getCleanupThresholdMilliseconds();

        int removedCount = 0;
        Iterator<Map.Entry<String, Long>> iterator = recentRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (entry.getValue() < thresholdTime) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("오래된 요청 기록 정리 완료 - 삭제된 기록: {}개, 남은 기록: {}개, 기준: {}분 전",
                    removedCount, recentRequests.size(), DuplicatePreventionConfig.CLEANUP_THRESHOLD_MINUTES);
        }
    }
}

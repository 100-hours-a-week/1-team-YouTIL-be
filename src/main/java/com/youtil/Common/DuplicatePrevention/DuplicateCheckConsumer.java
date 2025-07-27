package com.youtil.Common.DuplicatePrevention;

import com.fasterxml.jackson.core.JsonProcessingException;
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

    // 사용자별 최근 요청 기록
    private final ConcurrentHashMap<String, Long> recentRequests = new ConcurrentHashMap<>();

    /**
     * 설정된 토픽에서 중복 체크 요청 처리
     */
    @KafkaListener(
            topics = "${spring.kafka.consumers.duplicate.check.topic}",
            groupId = "${spring.kafka.consumers.duplicate.check.group-id}"
    )
    public void handleDuplicateCheckRequest(ConsumerRecord<String, String> record) {
        String requestId = record.key();
        long startTime = System.currentTimeMillis();

        try {
            // 기본 검증
            if (requestId == null || requestId.trim().isEmpty()) {
                log.warn("빈 requestId 수신");
                return;
            }

            // JSON 파싱
            DuplicateCheckRequest request;
            try {
                request = objectMapper.readValue(record.value(), DuplicateCheckRequest.class);
            } catch (JsonProcessingException e) {
                log.error("JSON 파싱 실패 - requestId: {}, payload: {}", requestId, record.value(), e);
                sendDefaultResponse(requestId, false); // 파싱 실패시 실패
                return;
            }

            log.info("중복 방지 요청 수신 - action: {}, userId: {}, requestId: {}",
                    request.getAction(), request.getUserId(), requestId);

            // 요청 검증
            if (!validateRequest(request, requestId)) {
                sendDefaultResponse(requestId, false); // 검증 실패시 실패
                return;
            }

            // 중복 체크 수행
            boolean isAllowed;
            try {
                isAllowed = checkDuplicate(request);
            } catch (Exception e) {
                log.error("중복 체크 로직 실행 실패 - requestId: {}", requestId, e);
                sendDefaultResponse(requestId, true);
                return;
            }

            // 응답 전송
            try {
                duplicatePreventionManager.handleResponse(requestId, isAllowed);

                long processingTime = System.currentTimeMillis() - startTime;
                log.info("중복 방지 처리 완료 - requestId: {}, allowed: {}, time: {}ms",
                        requestId, isAllowed, processingTime);
            } catch (Exception e) {
                log.error("응답 전송 실패 - requestId: {}, result: {}", requestId, isAllowed, e);
            }

        } catch (Exception e) {
            log.error("예상치 못한 오류 - requestId: {}", requestId, e);
            sendDefaultResponse(requestId, true);
        }
    }

    /**
     * 요청 검증 로직
     */
    private boolean validateRequest(DuplicateCheckRequest request, String requestId) {
        // 기본 null 체크
        if (request == null) {
            log.warn("null 요청 객체 - requestId: {}", requestId);
            return false;
        }

        // requestId 일치 검증
        if (!requestId.equals(request.getRequestId())) {
            log.warn("requestId 불일치 - key: {}, payload: {}", requestId, request.getRequestId());
            return false;
        }

        // 필드 검증
        if (request.getUserId() == null || request.getUserId() <= 0) {
            log.warn("잘못된 userId - requestId: {}, userId: {}", requestId, request.getUserId());
            return false;
        }

        if (request.getAction() == null || request.getAction().trim().isEmpty()) {
            log.warn("빈 action - requestId: {}", requestId);
            return false;
        }

        if (request.getDataHash() == null || request.getDataHash().trim().isEmpty()) {
            log.warn("빈 dataHash - requestId: {}", requestId);
            return false;
        }

        // 타임스탬프 검증
        long currentTime = System.currentTimeMillis();
        long timeDiff = Math.abs(currentTime - request.getTimestamp());
        if (timeDiff > 300_000) {
            log.warn("오래된 요청 - requestId: {}, timeDiff: {}ms", requestId, timeDiff);
            return false;
        }

        return true;
    }

    /**
     * 실제 중복 체크 로직
     */
    private boolean checkDuplicate(DuplicateCheckRequest request) {
        String key = String.format("%d:%s:%s",
                request.getUserId(), request.getAction(), request.getDataHash());

        long currentTime = System.currentTimeMillis();
        int limitMilliseconds = DuplicatePreventionConfig.getLimitMilliseconds(request.getAction());

        Long resultTime = recentRequests.compute(key, (k, lastTime) -> {
            if (lastTime != null && (currentTime - lastTime) < limitMilliseconds) {
                // 기존 시간 유지
                return lastTime;
            }
            // 시간 업데이트
            return currentTime;
        });

        boolean isAllowed = resultTime.equals(currentTime);

        if (!isAllowed) {
            long timeDiff = currentTime - resultTime;
            log.warn("중복 요청 감지 - key: {}, 이전요청: {}ms 전", key, timeDiff);
        }

        // 메모리 관리
        if (recentRequests.size() > DuplicatePreventionConfig.MAX_REQUESTS_IN_MEMORY) {
            cleanupOldRequests(currentTime);
        }

        return isAllowed;
    }

    /**
     * 오래된 요청 기록 정리
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
            log.info("메모리 정리 완료 - 삭제: {}개, 잔여: {}개, 기준: {}분 전",
                    removedCount, recentRequests.size(),
                    DuplicatePreventionConfig.CLEANUP_THRESHOLD_MINUTES);
        }
    }

    /**
     * 기본 응답 전송
     */
    private void sendDefaultResponse(String requestId, boolean defaultResult) {
        if (requestId == null) return;

        try {
            duplicatePreventionManager.handleResponse(requestId, defaultResult);
            log.debug("기본 응답 전송 완료 - requestId: {}, result: {}", requestId, defaultResult);
        } catch (Exception e) {
            log.error("기본 응답 전송 실패 - requestId: {}", requestId, e);
        }
    }
}

package com.youtil.Common.DuplicatePrevention;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Common.Dto.DuplicateCheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicatePreventionManager {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> pendingRequests = new ConcurrentHashMap<>();

    public boolean preventGuestbookDuplicate(Long guestId, Long ownerId, String content, Object originalData) {
        if (!isValidGuestbookParams(guestId, ownerId, content)) {
            log.warn("잘못된 방명록 파라미터 - guestId: {}, ownerId: {}", guestId, ownerId);
            return true;
        }

        String dataHash = createDataHash(ownerId.toString(), content);
        return executePreventDuplicate("guestbook", guestId, dataHash, originalData);
    }

    public boolean preventTilRecommendDuplicate(Long userId, Long tilId, Object originalData) {
        if (!isValidTilRecommendParams(userId, tilId)) {
            log.warn("잘못된 TIL 추천 파라미터 - userId: {}, tilId: {}", userId, tilId);
            return true;
        }

        String dataHash = tilId.toString();
        return executePreventDuplicate("til_recommend", userId, dataHash, originalData);
    }

    public boolean preventCommentDuplicate(Long userId, Long tilId, String content, Object originalData) {
        if (!isValidCommentParams(userId, tilId, content)) {
            log.warn("잘못된 댓글 파라미터 - userId: {}, tilId: {}", userId, tilId);
            return true;
        }

        String dataHash = createDataHash(tilId.toString(), content);
        return executePreventDuplicate("comment", userId, dataHash, originalData);
    }

    /**
     * 인터셉터에서 사용할 범용 중복 방지 메서드
     */
    public boolean preventDuplicate(String action, Long userId, String dataHash, Object originalData) {
        if (!isValidGenericParams(userId, action, dataHash)) {
            log.warn("잘못된 범용 중복 방지 파라미터 - userId: {}, action: {}", userId, action);
            return true;
        }

        return executePreventDuplicate(action, userId, dataHash, originalData);
    }

    /**
     * 실제 중복 방지 로직을 수행하는 private 메서드
     */
    private boolean executePreventDuplicate(String action, Long userId, String dataHash, Object originalData) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 카프카 요청 메시지 생성
            DuplicateCheckRequest request = DuplicateCheckRequest.builder()
                    .requestId(requestId)
                    .action(action)
                    .userId(userId)
                    .dataHash(dataHash)
                    .timestamp(System.currentTimeMillis())
                    .originalData(originalData)
                    .build();

            CompletableFuture<Boolean> responseFuture = new CompletableFuture<>();
            pendingRequests.put(requestId, responseFuture);

            String jsonPayload = objectMapper.writeValueAsString(request);
            kafkaTemplate.send(DuplicatePreventionConfig.DUPLICATE_CHECK_TOPIC, requestId, jsonPayload);

            log.info("중복 방지 요청 전송 - action: {}, userId: {}, requestId: {}", action, userId, requestId);

            // 응답 대기
            Boolean result = responseFuture.get(DuplicatePreventionConfig.RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingRequests.remove(requestId);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("중복 요청 제어 {} - action: {}, userId: {}, 처리시간: {}ms",
                    result ? "통과" : "발동", action, userId, processingTime);

            return result;

        } catch (JsonProcessingException e) {
            log.error("JSON 직렬화 실패 - action: {}, userId: {}", action, userId, e);
            cleanupRequest(requestId);
            return true;
        } catch (TimeoutException e) {
            log.error("응답 시간 초과 - action: {}, userId: {}, requestId: {}", action, userId, requestId);
            cleanupRequest(requestId);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("스레드 중단됨 - action: {}, userId: {}", action, userId);
            cleanupRequest(requestId);
            return true;
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생 - action: {}, userId: {}", action, userId, e);
            cleanupRequest(requestId);
            return true;
        }
    }

    /**
     * 카프카 컨슈머에서 호출할 응답 처리 메서드
     */
    public void handleResponse(String requestId, boolean allowed) {
        if (requestId == null) return;

        CompletableFuture<Boolean> future = pendingRequests.get(requestId);
        if (future != null) {
            future.complete(allowed);
            log.debug("응답 처리 완료 - requestId: {}, allowed: {}", requestId, allowed);
        } else {
            log.warn("대기 중이지 않은 요청 응답 수신 - requestId: {}", requestId);
        }
    }

    private void cleanupRequest(String requestId) {
        if (requestId != null) {
            CompletableFuture<Boolean> removed = pendingRequests.remove(requestId);
            if (removed != null && !removed.isDone()) {
                removed.cancel(true);
            }
        }
    }

    private String createDataHash(String... data) {
        if (data == null || data.length == 0) return "0";

        try {
            String combined = String.join(":", data);
            return String.valueOf(Math.abs(combined.hashCode()));
        } catch (Exception e) {
            log.warn("해시 생성 실패, 기본값 사용", e);
            return "0";
        }
    }

    private boolean isValidGuestbookParams(Long guestId, Long ownerId, String content) {
        return guestId != null && guestId > 0
                && ownerId != null && ownerId > 0
                && content != null && !content.trim().isEmpty() && content.length() <= 100;
    }

    private boolean isValidTilRecommendParams(Long userId, Long tilId) {
        return userId != null && userId > 0 && tilId != null && tilId > 0;
    }

    private boolean isValidCommentParams(Long userId, Long tilId, String content) {
        return userId != null && userId > 0
                && tilId != null && tilId > 0
                && content != null && !content.trim().isEmpty() && content.length() <= 500;
    }

    private boolean isValidGenericParams(Long userId, String action, String dataHash) {
        return userId != null && userId > 0
                && action != null && !action.trim().isEmpty()
                && dataHash != null && !dataHash.trim().isEmpty();
    }
}

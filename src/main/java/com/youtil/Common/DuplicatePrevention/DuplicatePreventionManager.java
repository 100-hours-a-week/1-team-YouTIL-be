package com.youtil.Common.DuplicatePrevention;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicatePreventionManager {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 대기 중인 요청들을 관리하는 맵
    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> pendingRequests = new ConcurrentHashMap<>();

    /**
     * 방명록 작성
     */
    public boolean preventGuestbookDuplicate(Long guestId, Long ownerId, String content, Object originalData) {
        String dataHash = createDataHash(ownerId.toString(), content);
        return preventDuplicate("guestbook", guestId, dataHash, originalData);
    }

    /**
     * TIL 추천
     */
    public boolean preventTilRecommendDuplicate(Long userId, Long tilId, Object originalData) {
        String dataHash = tilId.toString();
        return preventDuplicate("til_recommend", userId, dataHash, originalData);
    }

    /**
     * 댓글 작성
     */
    public boolean preventCommentDuplicate(Long userId, Long tilId, String content, Object originalData) {
        String dataHash = createDataHash(tilId.toString(), content);
        return preventDuplicate("comment", userId, dataHash, originalData);
    }

    /**
     * 공통 중복 방지 로직
     */
    private boolean preventDuplicate(String action, Long userId, String dataHash, Object originalData) {
        String requestId = UUID.randomUUID().toString();

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

            // 응답 대기용 CompletableFuture 생성
            CompletableFuture<Boolean> responseFuture = new CompletableFuture<>();
            pendingRequests.put(requestId, responseFuture);

            // 설정된 토픽으로 중복 체크 요청 전송
            String jsonPayload = objectMapper.writeValueAsString(request);
            kafkaTemplate.send(DuplicatePreventionConfig.DUPLICATE_CHECK_TOPIC, requestId, jsonPayload);

            log.info("중복 방지 요청 전송 - action: {}, userId: {}, requestId: {}", action, userId, requestId);

            // 설정된 시간만큼 응답 대기
            Boolean result = responseFuture.get(
                    DuplicatePreventionConfig.RESPONSE_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            // 대기 맵에서 제거
            pendingRequests.remove(requestId);

            if (result) {
                log.info("중복 요청 제어 통과 - action: {}, userId: {}, requestId: {}", action, userId, requestId);
            } else {
                log.warn("중복 요청 제어 발동 - action: {}, userId: {}", action, userId);
            }

            return result;

        } catch (Exception e) {
            log.error("중복 요청 제어 처리 중 오류 발생: {}", e.getMessage(), e);
            // 대기 맵에서 제거
            pendingRequests.remove(requestId);
            // 오류 발생시 안전하게 허용 (서비스 중단 방지)
            return true;
        }
    }

    /**
     * 카프카 컨슈머에서 호출할 응답 처리 메서드
     */
    public void handleResponse(String requestId, boolean allowed) {
        CompletableFuture<Boolean> future = pendingRequests.get(requestId);
        if (future != null) {
            future.complete(allowed);
            log.debug("응답 처리 완료 - requestId: {}, allowed: {}", requestId, allowed);
        } else {
            log.warn("대기 중이지 않은 요청 응답 수신 - requestId: {}", requestId);
        }
    }

    /**
     * 데이터 해시 생성 (내용 기반)
     */
    private String createDataHash(String... data) {
        String combined = String.join(":", data);
        return String.valueOf(combined.hashCode());
    }
}

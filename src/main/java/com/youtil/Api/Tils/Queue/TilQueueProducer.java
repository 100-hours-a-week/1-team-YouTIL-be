package com.youtil.Api.Tils.Queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TilQueueProducer {

    private static final String STREAM_KEY = "ai:til:stream";
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public String enqueueTilRequest(Long userId, TilRequestDTO.CreateWithAiRequest request) {
        String requestId = UUID.randomUUID().toString();

        try {
            Map<String, String> payload = Map.of(
                    "requestId", requestId,
                    "userId", userId.toString(),
                    "requestJson", objectMapper.writeValueAsString(request)
            );

            stringRedisTemplate.opsForStream()
                    .add(StreamRecords.mapBacked(payload).withStreamKey(STREAM_KEY));

            return requestId;

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("TIL 요청 직렬화 실패: " + e.getMessage());
        }
    }
}

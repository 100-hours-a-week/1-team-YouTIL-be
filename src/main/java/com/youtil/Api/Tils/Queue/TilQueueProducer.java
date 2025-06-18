package com.youtil.Api.Tils.Queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Exception.TilException.TilException.TilSerializationException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TilQueueProducer {


    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    @Qualifier("tilServiceConstants")
    private final AiServiceConstants tilServiceConstants;

    public String enqueueTilRequest(Long userId, TilRequestDTO.CreateWithAiRequest request) {
        String requestId = UUID.randomUUID().toString();

        try {
            Map<String, String> payload = Map.of(
                    tilServiceConstants.getRequestIdKey(), requestId,
                    tilServiceConstants.getUserIdKey(), userId.toString(),
                    tilServiceConstants.getRequestJsonKey(),
                    objectMapper.writeValueAsString(request)
            );

            stringRedisTemplate.opsForStream()
                    .add(StreamRecords.mapBacked(payload)
                            .withStreamKey(tilServiceConstants.getStreamKey()));

            return requestId;

        } catch (JsonProcessingException e) {
            throw new TilSerializationException();
        }
    }
}

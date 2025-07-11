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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TilQueueProducer {


    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Qualifier("tilServiceConstants")
    private final AiServiceConstants tilServiceConstants;

    public String enqueueTilRequest(Long userId, TilRequestDTO.CreateWithAiRequest request) {
        String requestId = UUID.randomUUID().toString();
        String enqueueTime = String.valueOf(System.currentTimeMillis());
        try {
            Map<String, String> payload = Map.of(
                    tilServiceConstants.getRequestIdKey(), requestId,
                    tilServiceConstants.getUserIdKey(), userId.toString(),
                    tilServiceConstants.getRequestJsonKey(),
                    objectMapper.writeValueAsString(request),
                    "enqueueTime", enqueueTime
            );
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(tilServiceConstants.getStreamKey(), requestId, jsonPayload);

            return requestId;

        } catch (JsonProcessingException e) {
            throw new TilSerializationException();
        }
    }
}


package com.youtil.Api.Filtering.Queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Filtering.Dto.FilterRequestDto;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Exception.InterviewException.InterviewException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilterQueueProducer {

    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Qualifier("filterServiceConstants")
    private final AiServiceConstants filterServiceConstants;

    public String enqueueFilterRequest(Long userId,
            FilterRequestDto request) {
        String requestId = UUID.randomUUID().toString();
        String enqueueTime = String.valueOf(System.currentTimeMillis());
        try {
            Map<String, String> payload = Map.of(
                    filterServiceConstants.getRequestIdKey(), requestId,
                    filterServiceConstants.getUserIdKey(), userId.toString(),
                    filterServiceConstants.getRequestJsonKey(),
                    objectMapper.writeValueAsString(request),
                    "enqueueTime", enqueueTime
            );
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(filterServiceConstants.getStreamKey(), requestId, jsonPayload);

            return requestId;

        } catch (JsonProcessingException e) {
            throw new InterviewException.InterviewSerializationException();
        }
    }
}

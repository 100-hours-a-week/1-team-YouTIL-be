package com.youtil.Api.Interview.Queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Interview.dto.InterviewRequestDTO;
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
public class InterviewQueueProducer {


    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    @Qualifier("interviewServiceConstants")
    private final AiServiceConstants interviewServiceConstants;

    public String enqueueInterviewRequest(Long userId,
            InterviewRequestDTO.CreateInterviewRequest request) {
        String requestId = UUID.randomUUID().toString();
        String enqueueTime = String.valueOf(System.currentTimeMillis());
        try {
            Map<String, String> payload = Map.of(
                    interviewServiceConstants.getRequestIdKey(), requestId,
                    interviewServiceConstants.getUserIdKey(), userId.toString(),
                    interviewServiceConstants.getRequestJsonKey(),
                    objectMapper.writeValueAsString(request),
                    "enqueueTime", enqueueTime
            );
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(interviewServiceConstants.getStreamKey(), requestId, jsonPayload);

            return requestId;

        } catch (JsonProcessingException e) {
            throw new InterviewException.InterviewSerializationException();
        }
    }
}

package com.youtil.Api.Interview.Queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Interview.dto.InterviewRequestDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Exception.InterviewException.InterviewException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static com.youtil.Common.Constants.InterviewServiceConstans.*;

@Component
@RequiredArgsConstructor
public class InterviewQueueProducer {


    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public String enqueueInterviewRequest(Long userId, InterviewRequestDTO.CreateInterviewRequest request) {
        String requestId = UUID.randomUUID().toString();

        try {
            Map<String, String> payload = Map.of(
                    REQUEST_ID_KEY, requestId,
                    USER_ID_KEY, userId.toString(),
                    REQUEST_JSON_KEY, objectMapper.writeValueAsString(request)
            );

            stringRedisTemplate.opsForStream()
                    .add(StreamRecords.mapBacked(payload).withStreamKey(STREAM_KEY));

            return requestId;

        } catch (JsonProcessingException e) {
            throw new InterviewException.InterviewSerializationException();
        }
    }
}

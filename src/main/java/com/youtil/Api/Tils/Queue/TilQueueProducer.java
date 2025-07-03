package com.youtil.Api.Tils.Queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import static com.youtil.Common.Constants.TilServiceConstants.REQUEST_ID_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.REQUEST_JSON_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.STREAM_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.USER_ID_KEY;
import com.youtil.Exception.TilException.TilException.TilSerializationException;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TilQueueProducer {


    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public String enqueueTilRequest(Long userId, TilRequestDTO.CreateWithAiRequest request) {
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
            throw new TilSerializationException();
        }
    }
}

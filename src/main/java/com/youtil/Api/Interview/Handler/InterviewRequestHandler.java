package com.youtil.Api.Interview.Handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Interview.Service.InterViewService;
import com.youtil.Api.Interview.dto.InterviewRequestDTO;
import com.youtil.Api.Interview.dto.InterviewResponseDTO;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.CreateInterviewResponseDTO;
import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Handler.AbstractAiRequestHandler;
import com.youtil.Concurrency.RedisSemaphoreManager;
import java.util.concurrent.PriorityBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j

public class InterviewRequestHandler extends
        AbstractAiRequestHandler<CreateInterviewResponseDTO, PrioritizedInterviewRequest> {

    private final InterViewService interviewService;

    public InterviewRequestHandler(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisSemaphoreManager semaphoreManager,
            PriorityBlockingQueue<PrioritizedInterviewRequest> processingQueue,
            @Qualifier("interviewServiceConstants") AiServiceConstants constants,
            InterViewService interviewService) {
        super(redisTemplate, objectMapper, semaphoreManager, processingQueue, constants);
        this.interviewService = interviewService;
    }

    @Override
    protected String getAiType() {
        return AiType.INTERVIEW.toString();
    }

    @Override
    protected InterviewResponseDTO.CreateInterviewResponseDTO handleRequest(String requestJson,
            long userId) throws Exception {
        InterviewRequestDTO.CreateInterviewRequest request = objectMapper.readValue(requestJson,
                InterviewRequestDTO.CreateInterviewRequest.class);
        Long interviewId = interviewService.createInterview(request, userId);
        return InterviewResponseDTO.CreateInterviewResponseDTO.builder().interviewId(interviewId)
                .build();
    }

    @Override
    protected void logSuccess(String requestId) {
        log.info("Interview 생성 완료: {}", requestId);
    }

    @Override
    protected void setErrorResult(String requestId) {
        InterviewResponseDTO.CreateInterviewResponseDTO errorResponse = InterviewResponseDTO.CreateInterviewResponseDTO.builder()
                .interviewId(null)
                .build();

        try {
            redisTemplate.opsForValue().set(
                    constants.getResultKey() + requestId,
                    objectMapper.writeValueAsString(errorResponse),
                    constants.getResultTtl());
        } catch (JsonProcessingException e) {
            log.error("면접 에러 응답 저장 실패", e);
        }
    }

    @Override
    protected PrioritizedInterviewRequest wrap(MapRecord<String, Object, Object> record) {
        return new PrioritizedInterviewRequest(record);
    }
}

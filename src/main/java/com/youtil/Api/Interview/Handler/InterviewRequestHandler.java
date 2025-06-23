package com.youtil.Api.Interview.Handler;

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
import java.util.concurrent.ScheduledExecutorService;
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
            @Qualifier("interviewServiceConstants")
            AiServiceConstants constants,
            ScheduledExecutorService aiRequestScheduler,
            InterViewService interviewService) {

        super(redisTemplate, objectMapper, semaphoreManager, processingQueue, constants,
                aiRequestScheduler);
        this.interviewService = interviewService;
    }

    @Override
    protected String getAiType() {
        return AiType.INTERVIEW.name();
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
    protected Object getEmptyErrorResponse() {
        return InterviewResponseDTO.CreateInterviewResponseDTO.builder()
                .interviewId(null)
                .build();
    }

    @Override
    protected PrioritizedInterviewRequest wrap(MapRecord<String, Object, Object> record) {
        return new PrioritizedInterviewRequest(record);
    }
}

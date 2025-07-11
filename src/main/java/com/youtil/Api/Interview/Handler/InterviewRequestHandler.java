package com.youtil.Api.Interview.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Interview.Service.InterViewService;
import com.youtil.Api.Interview.dto.InterviewRequestDTO;
import com.youtil.Api.Interview.dto.InterviewResponseDTO.CreateInterviewResponseDTO;
import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.AiProgress;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Handler.AbstractAiRequestHandler;
import com.youtil.Common.Retry.RetryStrategy;
import com.youtil.Common.Sse.SseEmitterService;
import com.youtil.Concurrency.RedisSemaphoreManager;
import com.youtil.Concurrency.RedisSemaphoreManager.SemaphoreAcquireResult;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InterviewRequestHandler extends
        AbstractAiRequestHandler<CreateInterviewResponseDTO> {

    private final InterViewService interviewService;
    private final PriorityBlockingQueue<PrioritizedInterviewRequest> queue;

    public InterviewRequestHandler(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisSemaphoreManager semaphoreManager,
            @Qualifier("interviewServiceConstants")
            AiServiceConstants constants,
            @Qualifier("delayScheduler")
            ScheduledExecutorService aiRequestScheduler,
            InterViewService interviewService,
            @Qualifier("interviewRetryStrategy") RetryStrategy<String> retryStrategy,
            SseEmitterService sseEmitterService,
            PriorityBlockingQueue<PrioritizedInterviewRequest> queue
    ) {
        super(
                redisTemplate,
                objectMapper,
                semaphoreManager,
                aiRequestScheduler,
                constants,
                retryStrategy,
                sseEmitterService
        );
        this.queue = queue;
        this.interviewService = interviewService;
    }

    @Override
    protected String getAiType() {
        return AiType.INTERVIEW.name();
    }

    @Override
    protected CreateInterviewResponseDTO handleRequest(String requestJson, long userId,
            String requestId)
            throws Exception {

        InterviewRequestDTO.CreateInterviewRequest request = objectMapper.readValue(requestJson,
                InterviewRequestDTO.CreateInterviewRequest.class);
        Long interviewId = interviewService.createInterview(request, userId);
        return CreateInterviewResponseDTO.builder().interviewId(interviewId).build();
    }

    @Override
    protected void logSuccess(String requestId) {

        sseEmitterService.send(requestId, AiProgress.FINISHED, 0, 0);
        log.info("Interview 생성 완료: {}", requestId);
    }

    @Override
    protected Object getEmptyErrorResponse() {
        return CreateInterviewResponseDTO.builder().interviewId(null).build();
    }

    @Override
    public SemaphoreAcquireResult tryAcquire(String requestId) {
        return semaphoreManager.tryAcquireSemaphore(requestId, getAiType(), queue);
    }


    public void releaseSemaphore(String requestId) {
        semaphoreManager.releaseSemaphore(requestId, getAiType());
    }


    public void retry(PrioritizedInterviewRequest request, int retryCount) {
        retryStrategy.retry(
                request.getRequestJson(),
                request.getUserId(),
                request.getRequestId(),
                retryCount,
                this::setErrorResult
        );
    }
}

package com.youtil.Api.Filtering.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Filtering.Dto.FilterRequestDto;
import com.youtil.Api.Filtering.Dto.PrioritizedFilterReqeust;
import com.youtil.Api.Filtering.Service.ContentFilterService;
import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
import com.youtil.Common.Constants.AiServiceConstants;
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
public class FilterRequestHandler extends AbstractAiRequestHandler<Void> {

    private final PriorityBlockingQueue<PrioritizedInterviewRequest> queue;
    private final ContentFilterService contentFilterService;

    public FilterRequestHandler(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisSemaphoreManager semaphoreManager,
            @Qualifier("interviewServiceConstants")
            AiServiceConstants constants,
            @Qualifier("delayScheduler")
            ScheduledExecutorService aiRequestScheduler,
            ContentFilterService contentFilterService,
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
        this.contentFilterService = contentFilterService;
    }

    @Override
    protected String getAiType() {
        return AiType.INTERVIEW.name();
    }

    @Override
    protected Void handleRequest(String requestJson, long userId,
            String requestId)
            throws Exception {

        FilterRequestDto request = objectMapper.readValue(requestJson,
                FilterRequestDto.class);
        contentFilterService.CommentFilter(request.getId(), request.getContent(),
                request.getType());
        return null;
    }

    @Override
    protected void logSuccess(String requestId) {

        log.info("댓글 및 방명록 필터 완료: {}", requestId);
    }

    @Override
    protected Object getEmptyErrorResponse() {
        return FilterRequestDto.builder().build();
    }

    @Override
    public SemaphoreAcquireResult tryAcquire(String requestId) {
        return semaphoreManager.tryAcquireSemaphore(requestId, getAiType(), queue);
    }


    public void releaseSemaphore(String requestId) {
        semaphoreManager.releaseSemaphore(requestId, getAiType());
    }


    public void retry(PrioritizedFilterReqeust request, int retryCount) {
        retryStrategy.retry(
                request.getRequestJson(),
                request.getUserId(),
                request.getRequestId(),
                retryCount,
                this::setErrorResult
        );
    }
}

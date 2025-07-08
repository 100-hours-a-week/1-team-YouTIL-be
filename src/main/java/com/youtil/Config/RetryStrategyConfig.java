package com.youtil.Config;

import com.youtil.Api.Filtering.Dto.PrioritizedFilterReqeust;
import com.youtil.Api.Filtering.Handler.FilterRequestHandler;
import com.youtil.Api.Interview.Handler.InterviewRequestHandler;
import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Api.Tils.Handler.TilRequestHandler;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Retry.RetryStrategy;
import com.youtil.Common.Retry.RetryStrategyImpl;
import com.youtil.Concurrency.RedisSemaphoreManager;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@Slf4j
public class RetryStrategyConfig {

    @Bean
    public RetryStrategy<String> tilRetryStrategy(
            @Qualifier("delayScheduler") ScheduledExecutorService scheduler,
            @Qualifier("tilServiceConstants") AiServiceConstants constants,
            RedisSemaphoreManager semaphoreManager,
            @Lazy TilRequestHandler tilRequestHandler,
            PriorityBlockingQueue<PrioritizedTilRequest> queue
    ) {
        TriConsumer<String, Long, String> retryAction = (requestJson, userId, requestId) -> {
            try {
                tilRequestHandler.process(requestJson, userId, requestId);
            } catch (Exception e) {
                log.warn("RetryAction 실행 실패", e);
                tilRequestHandler.setErrorResult(requestId);
            }
        };
        return new RetryStrategyImpl(
                scheduler,
                constants,
                semaphoreManager,
                queue,
                AiType.TIL.name(),
                retryAction
        );
    }

    @Bean
    public RetryStrategy<String> interviewRetryStrategy(
            @Qualifier("delayScheduler") ScheduledExecutorService scheduler,
            @Qualifier("interviewServiceConstants") AiServiceConstants constants,
            RedisSemaphoreManager semaphoreManager,
            @Lazy InterviewRequestHandler handler,
            PriorityBlockingQueue<PrioritizedInterviewRequest> queue
    ) {
        TriConsumer<String, Long, String> retryAction = (requestJson, userId, requestId) -> {
            try {
                handler.process(requestJson, userId, requestId);
            } catch (Exception e) {
                log.warn("RetryAction 실행 실패", e);
                handler.setErrorResult(requestId);
            }
        };
        return new RetryStrategyImpl(
                scheduler,
                constants,
                semaphoreManager,
                queue,
                AiType.INTERVIEW.name(),
                retryAction
        );
    }

    @Bean
    public RetryStrategy<String> filterRetryStrategy(
            @Qualifier("delayScheduler") ScheduledExecutorService scheduler,
            @Qualifier("filterServiceConstants") AiServiceConstants constants,
            RedisSemaphoreManager semaphoreManager,
            @Lazy FilterRequestHandler handler,
            PriorityBlockingQueue<PrioritizedFilterReqeust> queue
    ) {
        TriConsumer<String, Long, String> retryAction = (requestJson, userId, requestId) -> {
            try {
                handler.process(requestJson, userId, requestId);
            } catch (Exception e) {
                log.warn("RetryAction 실행 실패", e);
                handler.setErrorResult(requestId);
            }
        };
        return new RetryStrategyImpl(
                scheduler,
                constants,
                semaphoreManager,
                queue,
                AiType.INTERVIEW.name(),
                retryAction
        );
    }
}

package com.youtil.Config;

import com.youtil.Api.Interview.Handler.InterviewRequestHandler;
import com.youtil.Api.Tils.Handler.TilRequestHandler;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Retry.RetryStrategy;
import com.youtil.Common.Retry.RetryStrategyImpl;
import com.youtil.Concurrency.RedisSemaphoreManager;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
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
            @Lazy TilRequestHandler tilRequestHandler
    ) {
        return new RetryStrategyImpl(
                scheduler,
                constants,
                semaphoreManager,
                AiType.TIL.name(),
                (requestJson, requestId, userId) -> {
                    try {
                        tilRequestHandler.process(requestJson, userId, requestId);
                    } catch (Exception e) {
                        log.warn("RetryAction 실행 실패 - requestId={}, error={}", requestId,
                                e.getMessage());
                        tilRequestHandler.setErrorResult(requestId); // 안전망
                    }
                }
        );
    }

    @Bean
    public RetryStrategy<String> interviewRetryStrategy(
            @Qualifier("delayScheduler") ScheduledExecutorService scheduler,
            @Qualifier("interviewServiceConstants") AiServiceConstants constants,
            RedisSemaphoreManager semaphoreManager,
            @Lazy InterviewRequestHandler handler
    ) {
        return new RetryStrategyImpl(
                scheduler,
                constants,
                semaphoreManager,
                AiType.INTERVIEW.name(),
                (requestJson, requestId, userId) -> {
                    try {
                        handler.process(requestJson, userId, requestId);
                    } catch (Exception e) {
                        log.warn("RetryAction 실행 실패 - requestId={}, error={}", requestId,
                                e.getMessage());
                        handler.setErrorResult(requestId); // 안전망
                    }
                } // retryAction으로 직접 주입
        );
    }
}

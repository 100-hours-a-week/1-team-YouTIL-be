package com.youtil.Config;

import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Retry.RetryStrategy;
import com.youtil.Common.Retry.RetryStrategyImpl;
import com.youtil.Concurrency.RedisSemaphoreManager;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryStrategyConfig {

    @Bean
    public RetryStrategy<PrioritizedInterviewRequest> interviewRetryStrategy(
            @Qualifier("delayScheduler") ScheduledExecutorService scheduler,
            @Qualifier("interviewServiceConstants") AiServiceConstants constants,
            RedisSemaphoreManager semaphoreManager,
            PriorityBlockingQueue<PrioritizedInterviewRequest> queue
    ) {
        return new RetryStrategyImpl<>(
                scheduler,
                queue,
                constants,
                semaphoreManager,
                AiType.INTERVIEW.name(),
                PrioritizedInterviewRequest::new
        );
    }

    @Bean
    public RetryStrategy<PrioritizedTilRequest> tilRetryStrategy(
            @Qualifier("delayScheduler") ScheduledExecutorService scheduler,
            PriorityBlockingQueue<PrioritizedTilRequest> queue,
            @Qualifier("tilServiceConstants") AiServiceConstants constants,
            RedisSemaphoreManager semaphoreManager
    ) {
        return new RetryStrategyImpl<>(
                scheduler,
                queue,
                constants,
                semaphoreManager,
                AiType.TIL.name(),
                PrioritizedTilRequest::new
        );
    }
}

package com.youtil.Config;

<

import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;

import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.PriorityBlockingQueue;

@Configuration
public class QueueConfig {

    @Bean
    public PriorityBlockingQueue<PrioritizedTilRequest> tilProcessingQueue() {
        return new PriorityBlockingQueue<>();
    }
    @Bean
    public PriorityBlockingQueue<PrioritizedInterviewRequest> tilProcessingQueue() {
        return new PriorityBlockingQueue<>();
    }

}

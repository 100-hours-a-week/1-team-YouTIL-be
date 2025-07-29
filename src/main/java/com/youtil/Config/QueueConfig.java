package com.youtil.Config;


import com.youtil.Api.Filtering.Dto.PrioritizedFilterReqeust;
import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import java.util.concurrent.PriorityBlockingQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    @Bean
    public PriorityBlockingQueue<PrioritizedTilRequest> tilProcessingQueue() {
        return new PriorityBlockingQueue<>();
    }

    @Bean
    public PriorityBlockingQueue<PrioritizedInterviewRequest> interviewProcessingQueue() {
        return new PriorityBlockingQueue<>();
    }
    @Bean
    public PriorityBlockingQueue<PrioritizedFilterReqeust> filterProcessingQueue() {
        return new PriorityBlockingQueue<>();
    }
}

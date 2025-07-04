package com.youtil.Config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchedulerConfig {

    @Bean(destroyMethod = "shutdown", name = "delayScheduler")
    public ScheduledExecutorService scheduler() {
        return Executors.newScheduledThreadPool(1);
    }
}

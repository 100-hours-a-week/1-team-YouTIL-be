package com.youtil.Config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.MapRecord;

@Configuration
public class QueueConfig {

    @Bean
    public BlockingQueue<MapRecord<String, Object, Object>> tilProcessingQueue() {
        return new LinkedBlockingQueue<>();
    }
}

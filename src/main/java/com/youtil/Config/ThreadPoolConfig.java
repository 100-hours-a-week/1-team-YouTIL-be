package com.youtil.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

import static com.youtil.Common.Constants.InterviewServiceConstans.INTERVIEW_WORKER_NAME;
import static com.youtil.Common.Constants.InterviewServiceConstans.MAX_INTERVIEW_WORKER_THREADS;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "interviewWorkerThreadPool")
    public ExecutorService interviewWorkerThreadPool() {
        int poolSize = MAX_INTERVIEW_WORKER_THREADS;
        return new ThreadPoolExecutor(
                poolSize,                    // corePoolSize
                poolSize,                    // maxPoolSize
                0L, TimeUnit.MILLISECONDS,   // keepAliveTime
                new LinkedBlockingQueue<>(), // 작업 큐 (무제한)
                new ThreadFactory() {
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, INTERVIEW_WORKER_NAME + count++);
                    }
                }
        );
    }
}

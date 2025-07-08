package com.youtil.Config;

import com.youtil.Common.Constants.AiServiceConstants;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "tilWorkerThreadPool")
    public ExecutorService tilWorkerThreadPool(
            @Qualifier("tilServiceConstants") AiServiceConstants tilServiceConstants) {
        int threadCount = tilServiceConstants.getMaxWorkerThreads(); // 예: 4
        return new ThreadPoolExecutor(
                threadCount,
                threadCount,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                tilServiceConstants.getWorkerThreadNamePrefix() + count++);
                    }
                }
        );
    }

    @Bean(name = "interviewWorkerThreadPool")
    public ExecutorService interviewWorkerThreadPool(
            @Qualifier("interviewServiceConstants") AiServiceConstants interviewServiceConstants) {
        int threadCount = interviewServiceConstants.getMaxWorkerThreads();
        return new ThreadPoolExecutor(
                threadCount,
                threadCount,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                interviewServiceConstants.getWorkerThreadNamePrefix() + count++);
                    }
                }
        );
    }

    @Bean(name = "filterWorkerThreadPool")
    public ExecutorService filterWorkerThreadPool(
            @Qualifier("filterServiceConstants") AiServiceConstants filterServiceConstants) {
        int threadCount = filterServiceConstants.getMaxWorkerThreads();
        return new ThreadPoolExecutor(
                threadCount,
                threadCount,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                filterServiceConstants.getWorkerThreadNamePrefix() + count++);
                    }
                }
        );
    }
}

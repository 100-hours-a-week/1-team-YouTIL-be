package com.youtil.Config;

import static com.youtil.Common.Constants.TilServiceConstants.MAX_TIL_WORKER_THREADS;
import static com.youtil.Common.Constants.TilServiceConstants.TIL_WORKER_NAME;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "tilWorkerThreadPool")
    public ExecutorService tilWorkerThreadPool() {
        int poolSize = MAX_TIL_WORKER_THREADS;
        return new ThreadPoolExecutor(
                poolSize,                    // corePoolSize
                poolSize,                    // maxPoolSize
                0L, TimeUnit.MILLISECONDS,   // keepAliveTime
                new LinkedBlockingQueue<>(), // 작업 큐 (무제한)
                new ThreadFactory() {
                    private int count = 0;

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, TIL_WORKER_NAME + count++);
                    }
                }
        );
    }
}

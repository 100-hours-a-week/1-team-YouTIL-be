package com.youtil.Api.Filtering.Handler;

import com.youtil.Api.Filtering.Dto.PrioritizedFilterReqeust;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Sse.SseEmitterService;
import com.youtil.Concurrency.RedisSemaphoreManager;
import com.youtil.Concurrency.RedisSemaphoreManager.SemaphoreAcquireResult;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FilterDispatcherWorker {

    private final PriorityBlockingQueue<PrioritizedFilterReqeust> processingQueue;
    private final ExecutorService executorService;
    private final FilterRequestHandler filterRequestHandlerr;
    private final SseEmitterService sseEmitterService;
    private final RedisSemaphoreManager semaphoreManager;

    public FilterDispatcherWorker(
            PriorityBlockingQueue<PrioritizedFilterReqeust> processingQueue,
            @Qualifier("filterWorkerThreadPool") ExecutorService executorService,
            FilterRequestHandler filterRequestHandlerr,
            SseEmitterService sseEmitterService,
            RedisSemaphoreManager semaphoreManager
    ) {
        this.processingQueue = processingQueue;
        this.executorService = executorService;
        this.filterRequestHandlerr = filterRequestHandlerr;
        this.sseEmitterService = sseEmitterService;
        this.semaphoreManager = semaphoreManager;
    }

    @PostConstruct
    public void startDispatcherThread() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {

                    PrioritizedFilterReqeust request = processingQueue.peek();
                    if (request == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    SemaphoreAcquireResult result = semaphoreManager.tryAcquireSemaphore(
                            request.getRequestId(),
                            AiType.FILTER.name(),
                            processingQueue
                    );

                    if (!result.acquired()) {
                        if ("none".equals(result.acquireType())) {

                            Thread.sleep(1000);
                            continue;
                        }
                    }
                    // 작업 처리를 위해 실제로 큐에서 꺼냄
                    processingQueue.poll();

                    executorService.submit(() -> {
                        try {
                            log.info("필터링 시작 : {}", request.getRequestId());
                            filterRequestHandlerr.handleRequestProcess(
                                    request.getRequestJson(),
                                    request.getUserId(),
                                    request.getRequestId()
                            );
                            request.getAck().acknowledge();
                        } catch (Exception e) {
                            log.error("필터링 실패={}", request.getRequestId(), e);
                            filterRequestHandlerr.retry(request, 1);
                        } finally {
                            filterRequestHandlerr.releaseSemaphore(request.getRequestId());
                        }
                    });

                } catch (Exception e) {
                    log.error("Dispatcher 오류", e);
                }
            }
        }, "interview-dispatcher-thread").start();
    }
}

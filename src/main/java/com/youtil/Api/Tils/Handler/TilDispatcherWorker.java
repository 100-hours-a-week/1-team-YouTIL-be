package com.youtil.Api.Tils.Handler;

import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Common.Enums.AiProgress;
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

public class TilDispatcherWorker {


    private final PriorityBlockingQueue<PrioritizedTilRequest> processingQueue;
    private final ExecutorService executorService;
    private final TilRequestHandler tilRequestHandler;
    private final SseEmitterService sseEmitterService;
    private final RedisSemaphoreManager semaphoreManager;

    public TilDispatcherWorker(
            PriorityBlockingQueue<PrioritizedTilRequest> processingQueue,
            @Qualifier("tilWorkerThreadPool") ExecutorService executorService,
            TilRequestHandler tilRequestHandler,
            SseEmitterService sseEmitterService,
            RedisSemaphoreManager semaphoreManager
    ) {
        this.processingQueue = processingQueue;
        this.executorService = executorService;
        this.tilRequestHandler = tilRequestHandler;
        this.sseEmitterService = sseEmitterService;
        this.semaphoreManager = semaphoreManager;
    }

    @PostConstruct
    public void startDispatcherThread() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {

                    PrioritizedTilRequest request = processingQueue.peek();
                    if (request == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    SemaphoreAcquireResult result = semaphoreManager.tryAcquireSemaphore(
                            request.getRequestId(),
                            AiType.TIL.name(),
                            processingQueue
                    );

                    if (!result.acquired()) {
                        if ("none".equals(result.acquireType())) {
                            int waitingCount = processingQueue.size();
                            int position = 1;

                            for (PrioritizedTilRequest r : processingQueue) {
                                sseEmitterService.send(r.getRequestId(), AiProgress.WAITING,
                                        position++, waitingCount);
                            }

                            Thread.sleep(1000);
                            continue;
                        }
                    }
                    // 작업 처리를 위해 실제로 큐에서 꺼냄
                    processingQueue.poll();

                    executorService.submit(() -> {
                        try {
                            tilRequestHandler.handleRequestProcess(
                                    request.getRequestJson(),
                                    request.getUserId(),
                                    request.getRequestId()
                            );
                            request.getAck().acknowledge();
                        } catch (Exception e) {
                            log.error("TIL 처리 실패 - requestId={}", request.getRequestId(), e);
                            tilRequestHandler.retry(request, 1);
                        } finally {
                            tilRequestHandler.releaseSemaphore(request.getRequestId());
                        }
                    });

                } catch (Exception e) {
                    log.error("Dispatcher 오류", e);
                }
            }
        }, "til-dispatcher-thread").start();
    }
}

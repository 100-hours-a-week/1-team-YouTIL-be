package com.youtil.Api.Tils.Handler;

import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
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

    public TilDispatcherWorker(
            PriorityBlockingQueue<PrioritizedTilRequest> processingQueue,
            @Qualifier("tilWorkerThreadPool") ExecutorService executorService,
            TilRequestHandler tilRequestHandler
    ) {
        this.processingQueue = processingQueue;
        this.executorService = executorService;
        this.tilRequestHandler = tilRequestHandler;
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

                    boolean acquired = tilRequestHandler.tryAcquireSemaphore(
                            request.getRequestId());
                    if (!acquired) {
                        Thread.sleep(100);
                        continue;
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

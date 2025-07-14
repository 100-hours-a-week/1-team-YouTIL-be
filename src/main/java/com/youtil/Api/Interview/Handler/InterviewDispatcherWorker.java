package com.youtil.Api.Interview.Handler;

import com.youtil.Api.Interview.dto.PrioritizedInterviewRequest;
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
public class InterviewDispatcherWorker {

    private final PriorityBlockingQueue<PrioritizedInterviewRequest> processingQueue;
    private final ExecutorService executorService;
    private final InterviewRequestHandler interviewRequestHandler;
    private final SseEmitterService sseEmitterService;
    private final RedisSemaphoreManager semaphoreManager;

    public InterviewDispatcherWorker(
            PriorityBlockingQueue<PrioritizedInterviewRequest> processingQueue,
            @Qualifier("interviewWorkerThreadPool") ExecutorService executorService,
            InterviewRequestHandler interviewRequestHandler,
            SseEmitterService sseEmitterService,
            RedisSemaphoreManager semaphoreManager
    ) {
        this.processingQueue = processingQueue;
        this.executorService = executorService;
        this.interviewRequestHandler = interviewRequestHandler;
        this.sseEmitterService = sseEmitterService;
        this.semaphoreManager = semaphoreManager;
    }

    @PostConstruct
    public void startDispatcherThread() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {

                    PrioritizedInterviewRequest request = processingQueue.peek();
                    if (request == null) {
                        Thread.sleep(50);
                        continue;
                    }

                    SemaphoreAcquireResult result = semaphoreManager.tryAcquireSemaphore(
                            request.getRequestId(),
                            AiType.INTERVIEW.name(),
                            processingQueue
                    );

                    if (!result.acquired()) {
                        if ("none".equals(result.acquireType())) {
                            int waitingCount = processingQueue.size();
                            int position = 1;

                            for (PrioritizedInterviewRequest r : processingQueue) {
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
                            log.info("면접 질문 생성 시작 - Request Id : {}", request.getRequestId());
                            interviewRequestHandler.handleRequestProcess(
                                    request.getRequestJson(),
                                    request.getUserId(),
                                    request.getRequestId()
                            );
                            request.getAck().acknowledge();
                        } catch (Exception e) {
                            log.error("면접 처리 실패 - requestId={}", request.getRequestId(), e);
                            sseEmitterService.send(request.getRequestId(), AiProgress.ERROR, 0, 0);
                            request.getAck().acknowledge();
//                            interviewRequestHandler.retry(request, 1);
                        } finally {
                            interviewRequestHandler.releaseSemaphore(request.getRequestId());
                        }
                    });

                } catch (Exception e) {
                    log.error("Dispatcher 오류", e);
                }
            }
        }, "interview-dispatcher-thread").start();
    }
}

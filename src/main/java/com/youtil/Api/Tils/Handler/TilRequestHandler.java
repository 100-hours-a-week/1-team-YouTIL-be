package com.youtil.Api.Tils.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Converter.TilDtoConverter;
import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO.CreateTilResponse;
import com.youtil.Api.Tils.Service.TilAiService;
import com.youtil.Api.Tils.Service.TilCommendService;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.AiProgress;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Handler.AbstractAiRequestHandler;
import com.youtil.Common.Retry.RetryStrategy;
import com.youtil.Common.Sse.SseEmitterService;
import com.youtil.Concurrency.RedisSemaphoreManager;
import com.youtil.Concurrency.RedisSemaphoreManager.SemaphoreAcquireResult;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TilRequestHandler extends AbstractAiRequestHandler<CreateTilResponse> {

    private final TilAiService tilAiService;
    private final TilCommendService tilCommendService;
    private final PriorityBlockingQueue<PrioritizedTilRequest> queue;

    public TilRequestHandler(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisSemaphoreManager semaphoreManager,
            @Qualifier("tilServiceConstants") AiServiceConstants constants,
            @Qualifier("delayScheduler") ScheduledExecutorService scheduler,
            @Qualifier("tilRetryStrategy") RetryStrategy<String> retryStrategy,
            TilAiService tilAiService,
            TilCommendService tilCommendService,
            SseEmitterService sseEmitterService,
            PriorityBlockingQueue<PrioritizedTilRequest> queue) {
        super(redisTemplate, objectMapper, semaphoreManager, scheduler, constants, retryStrategy,
                sseEmitterService);
        this.tilAiService = tilAiService;
        this.tilCommendService = tilCommendService;
        this.queue = queue;
    }

    @Override
    protected String getAiType() {
        return AiType.TIL.name();
    }

    @Override
    protected CreateTilResponse handleRequest(String requestJson, long userId, String requestId)
            throws Exception {

        TilRequestDTO.CreateWithAiRequest request =
                objectMapper.readValue(requestJson, TilRequestDTO.CreateWithAiRequest.class);

        // AI 서버에 간단한 형태로 요청
        TilAiResponseDTO aiResponse = tilAiService.generateTilContent(request, userId, requestId);

        // AI 응답을 기반으로 TIL 저장 요청 생성
        TilRequestDTO.CreateAiTilRequest saveRequest =
                TilDtoConverter.toCreateAiTilRequest(request, aiResponse);

        return tilCommendService.createTilFromAi(saveRequest, userId);
    }

    @Override
    protected void logSuccess(String requestId) {
        sseEmitterService.send(requestId, AiProgress.FINISHED, 0, 0);

        log.info("TIL 생성 완료: {}", requestId);
    }

    @Override
    protected Object getEmptyErrorResponse() {
        return CreateTilResponse.builder().tilID(null).build();
    }

    @Override
    public SemaphoreAcquireResult tryAcquire(String requestId) {
        return semaphoreManager.tryAcquireSemaphore(requestId, getAiType(), queue);
    }

    @Override
    public void process(String requestJson, Long userId, String requestId) {
        super.process(requestJson, userId, requestId);
    }

    public void releaseSemaphore(String requestId) {
        semaphoreManager.releaseSemaphore(requestId, getAiType());
    }


    public void retry(PrioritizedTilRequest request, int retryCount) {
        retryStrategy.retry(
                request.getRequestJson(),
                request.getUserId(),
                request.getRequestId(),
                retryCount,
                this::setErrorResult
        );
    }


}

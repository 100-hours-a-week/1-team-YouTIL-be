package com.youtil.Api.Tils.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Converter.TilDtoConverter;
import com.youtil.Api.Tils.Dto.PrioritizedTilRequest;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO.CreateTilResponse;
import com.youtil.Api.Tils.Service.TilAiService;
import com.youtil.Api.Tils.Service.TilCommendService;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Handler.AbstractAiRequestHandler;
import com.youtil.Common.Retry.RetryStrategy;
import com.youtil.Concurrency.RedisSemaphoreManager;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TilRequestHandler extends
        AbstractAiRequestHandler<CreateTilResponse, PrioritizedTilRequest> {

    private final TilAiService tilAiService;
    private final TilCommendService tilCommendService;

    public TilRequestHandler(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisSemaphoreManager semaphoreManager,
            PriorityBlockingQueue<PrioritizedTilRequest> processingQueue,
            @Qualifier("tilServiceConstants") AiServiceConstants constants,
            @Qualifier("delayScheduler") ScheduledExecutorService aiRequestScheduler,
            TilAiService tilAiService,
            TilCommendService tilCommendService,
            RetryStrategy<PrioritizedTilRequest> retryStrategy
    ) {
        super(redisTemplate, objectMapper, semaphoreManager, processingQueue, constants,
                aiRequestScheduler, retryStrategy);

        this.tilAiService = tilAiService;
        this.tilCommendService = tilCommendService;
    }

    @Override
    protected String getAiType() {
        return AiType.TIL.name();
    }

    @Override
    protected TilResponseDTO.CreateTilResponse handleRequest(String requestJson, long userId)
            throws Exception {
        TilRequestDTO.CreateWithAiRequest request =
                objectMapper.readValue(requestJson, TilRequestDTO.CreateWithAiRequest.class);

        // AI 서버에 간단한 형태로 요청
        TilAiResponseDTO aiResponse = tilAiService.generateTilContent(request, userId);

        // AI 응답을 기반으로 TIL 저장 요청 생성
        TilRequestDTO.CreateAiTilRequest saveRequest =
                TilDtoConverter.toCreateAiTilRequest(request, aiResponse);

        return tilCommendService.createTilFromAi(saveRequest, userId);
    }

    @Override
    protected void logSuccess(String requestId) {
        log.info("TIL 생성 완료: {}", requestId);
    }

    @Override
    protected Object getEmptyErrorResponse() {
        return TilResponseDTO.CreateTilResponse.builder()
                .tilID(null)
                .build();
    }

    @Override
    protected PrioritizedTilRequest wrap(MapRecord<String, Object, Object> record) {
        return new PrioritizedTilRequest(record);
    }
}

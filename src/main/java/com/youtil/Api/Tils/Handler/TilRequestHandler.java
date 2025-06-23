package com.youtil.Api.Tils.Handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitDetailService;
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
import com.youtil.Concurrency.RedisSemaphoreManager;
import io.jsonwebtoken.io.SerializationException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TilRequestHandler extends
        AbstractAiRequestHandler<CreateTilResponse, PrioritizedTilRequest> {

    private final TilAiService tilAiService;
    private final TilCommendService tilCommendService;
    private final GithubCommitDetailService githubCommitDetailService;

    public TilRequestHandler(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisSemaphoreManager semaphoreManager,
            PriorityBlockingQueue<PrioritizedTilRequest> processingQueue,
            @Qualifier("tilServiceConstants") AiServiceConstants constants,
            ScheduledExecutorService aiRequestScheduler,
            TilAiService tilAiService,
            TilCommendService tilCommendService,
            GithubCommitDetailService githubCommitDetailService) {

        super(redisTemplate, objectMapper, semaphoreManager, processingQueue, constants,
                aiRequestScheduler);

        this.tilAiService = tilAiService;
        this.tilCommendService = tilCommendService;
        this.githubCommitDetailService = githubCommitDetailService;
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

        CommitDetailRequestDTO.CommitDetailRequest commitRequest = new CommitDetailRequestDTO.CommitDetailRequest();
        commitRequest.setRepositoryId(request.getRepositoryId());
        commitRequest.setOrganizationId(request.getOrganizationId());
        commitRequest.setBranch(request.getBranch());
        commitRequest.setCommits(
                GitHubDtoConverter.toCommitDetailRequestSummaries(request.getCommits()));

        CommitDetailResponseDTO.CommitDetailResponse commitDetail =
                githubCommitDetailService.getCommitDetails(commitRequest, userId);

        TilAiResponseDTO aiResponse = tilAiService.generateTilContent(
                commitDetail, request.getRepositoryId(), request.getBranch(), request.getTitle());

        TilRequestDTO.CreateAiTilRequest saveRequest =
                TilDtoConverter.toCreateAiTilRequest(request, aiResponse);

        return tilCommendService.createTilFromAi(saveRequest, userId);
    }

    @Override
    protected void logSuccess(String requestId) {
        log.info("TIL 생성 완료: {}", requestId);
    }

    @Override
    protected void setErrorResult(String requestId) {
        TilResponseDTO.CreateTilResponse errorResponse = TilResponseDTO.CreateTilResponse.builder()
                .tilID(null).build();

        try {
            redisTemplate.opsForValue().set(
                    constants.getResultKey() + requestId,
                    objectMapper.writeValueAsString(errorResponse),
                    constants.getResultTtl());
        } catch (JsonProcessingException e) {
            log.error("에러 응답 저장 실패", e);
        } catch (RedisConnectionFailureException e) {
            log.error("레디스 접속 에러", e);
        } catch (SerializationException e) {
            log.error("직력화 실패", e);

        } catch (IllegalArgumentException e) {
            log.error("부적절한 값 포함", e);
        } catch (Exception e) {
            log.error("알수없는 예외가 발생했습니다.", e);
        }
    }

    @Override
    protected PrioritizedTilRequest wrap(MapRecord<String, Object, Object> record) {
        return new PrioritizedTilRequest(record);
    }
}

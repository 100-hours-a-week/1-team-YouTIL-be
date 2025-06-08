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
import com.youtil.Api.Tils.Service.TilAiService;
import com.youtil.Api.Tils.Service.TilCommendService;
import static com.youtil.Common.Constants.TilServiceConstants.GROUP;
import static com.youtil.Common.Constants.TilServiceConstants.OWNER_KEY_PREFIX;
import static com.youtil.Common.Constants.TilServiceConstants.OWNER_TTL;
import static com.youtil.Common.Constants.TilServiceConstants.REQUEST_ID_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.REQUEST_JSON_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.RESULT_ERROR_VALUE;
import static com.youtil.Common.Constants.TilServiceConstants.RESULT_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.RESULT_TTL;
import static com.youtil.Common.Constants.TilServiceConstants.RETRY_COUNT;
import static com.youtil.Common.Constants.TilServiceConstants.STREAM_KEY;
import static com.youtil.Common.Constants.TilServiceConstants.USER_ID_KEY;
import com.youtil.Util.RedisSemaphoreManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TilRequestHandler {


    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TilAiService tilAiService;
    private final TilCommendService tilCommendService;
    private final GithubCommitDetailService githubCommitDetailService;
    private final PriorityBlockingQueue<PrioritizedTilRequest> processingQueue;
    private final RedisSemaphoreManager semaphoreManager;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    public void process(MapRecord<String, Object, Object> record) {

        Map<Object, Object> data = record.getValue();
        String requestId = (String) data.get(REQUEST_ID_KEY);
        String userId = (String) data.get(USER_ID_KEY);
        String requestJson = (String) data.get(REQUEST_JSON_KEY);


        if (!tryAcquireOwnership(requestId)) {

            requeueWithDelay(record);
            return;
        }

        try {
            //소유권을 가지고 있는 워커가 해당 작업이 가능한지 확인
            if (!semaphoreManager.tryAcquireSemaphore(requestId)) {
                releaseOwnership(requestId);
                requeueWithDelay(record);
                return;
            }


            TilResponseDTO.CreateTilResponse response = handleTilCreation(requestJson,
                    Long.parseLong(userId));


            redisTemplate.opsForValue().set(RESULT_KEY + requestId,
                    objectMapper.writeValueAsString(response), RESULT_TTL);

            acknowledgeAndDelete(record);
            log.info("TIL 생성 완료: {}", requestId);

        } catch (Exception e) {
            //현재 재시도는 네트워크 에러에 한해서 최대 1회 재시도 요청 중
            log.error("TIL 처리 실패 - requestId={}, error={}", requestId, e.getMessage());
            handleRetry(record, data, requestId, e);

        } finally {
            releaseOwnership(requestId);
            semaphoreManager.releaseSemaphore(requestId);
        }

    }

    //에러코드를 통해 재시도를 해야할지 말아야할지 검증하는 메서드 (네트워크 에러로 고정)
    private boolean isRetryableException(Throwable e) {
        log.info("에러 발생 재 시도 검증");
        if (hasCause(e, WebClientRequestException.class)) {
            log.info("네트워크 에러로 재 시도");
            return true;
        }
        return false;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> clazz) {
        while (throwable != null) {
            if (clazz.isInstance(throwable)) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }


    private void acknowledgeAndDelete(MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
        redisTemplate.opsForStream().delete(STREAM_KEY, record.getId());
    }


    private void setErrorResult(String requestId) {
        redisTemplate.opsForValue().set(RESULT_KEY + requestId, RESULT_ERROR_VALUE, RESULT_TTL);
    }


    private void requeueWithDelay(MapRecord<String, Object, Object> record) {
        try {
            Thread.sleep(1000 + ThreadLocalRandom.current().nextInt(500));
        } catch (InterruptedException ignored) {
        }
        processingQueue.offer(new PrioritizedTilRequest(record));
    }


    private void handleRetry(MapRecord<String, Object, Object> record, Map<Object, Object> data,
            String requestId, Exception e) {
        int retryCount = Integer.parseInt(String.valueOf(data.getOrDefault(RETRY_COUNT, "0")));
        if (retryCount < 1 && isRetryableException(e)) {
            log.warn("500에러 - 재시도 예약: {}, count={}", requestId, retryCount + 1);
            Map<Object, Object> newData = new HashMap<>(data);
            newData.put(RETRY_COUNT, retryCount + 1);
            MapRecord<String, Object, Object> retryRecord = MapRecord.create(record.getStream(),
                    newData).withId(record.getId());

            if (semaphoreManager.tryAcquireSemaphore(requestId)) {
                //1.5초~2초 뒤에 실행되도록
                scheduler.schedule(() ->
                                processingQueue.offer(new PrioritizedTilRequest(retryRecord)),
                        1500 + ThreadLocalRandom.current().nextInt(500),
                        TimeUnit.MILLISECONDS
                );
            } else {
                log.info("재시도 직전 동시성 초과로 재시도 취소: {}", requestId);
                setErrorResult(requestId);
            }
        } else {
            setErrorResult(requestId);
        }

        acknowledgeAndDelete(record);
    }


    private TilResponseDTO.CreateTilResponse handleTilCreation(String requestJson, long userId)
            throws JsonProcessingException {
        TilRequestDTO.CreateWithAiRequest request =
                objectMapper.readValue(requestJson, TilRequestDTO.CreateWithAiRequest.class);

        CommitDetailRequestDTO.CommitDetailRequest commitRequest = new CommitDetailRequestDTO.CommitDetailRequest();
        commitRequest.setRepositoryId(request.getRepositoryId());
        commitRequest.setOrganizationId(request.getOrganizationId());
        commitRequest.setBranch(request.getBranch());
        commitRequest.setCommits(
                GitHubDtoConverter.toCommitDetailRequestSummaries(request.getCommits()));

        CommitDetailResponseDTO.CommitDetailResponse commitDetail =
                githubCommitDetailService.getCommitDetails(commitRequest,
                        userId);

        TilAiResponseDTO aiResponse = tilAiService.generateTilContent(
                commitDetail, request.getRepositoryId(), request.getBranch(),
                request.getTitle());

        TilRequestDTO.CreateAiTilRequest saveRequest =
                TilDtoConverter.toCreateAiTilRequest(request, aiResponse);

        return tilCommendService.createTilFromAi(saveRequest, userId);
    }

    //해당 워커쓰레드를 현재 작업의 소유자로 등록
    private boolean tryAcquireOwnership(String requestId) {
        String ownerKey = OWNER_KEY_PREFIX + requestId;
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(ownerKey, Thread.currentThread().getName(), OWNER_TTL));
    }


    private void releaseOwnership(String requestId) {
        redisTemplate.delete(OWNER_KEY_PREFIX + requestId);
    }
}


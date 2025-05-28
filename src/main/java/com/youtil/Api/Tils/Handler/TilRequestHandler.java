package com.youtil.Api.Tils.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Github.Converter.GitHubDtoConverter;
import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Service.GithubCommitDetailService;
import com.youtil.Api.Tils.Converter.TilDtoConverter;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Service.TilAiService;
import com.youtil.Api.Tils.Service.TilCommendService;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TilRequestHandler {

    private static final String STREAM_KEY = "ai:til:stream";
    private static final String GROUP = "ai-group";
    private static final String ACTIVE_SET_KEY = "ai:til:active_set";
    private static final Duration RESULT_TTL = Duration.ofMinutes(5);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final TilAiService tilAiService;
    private final TilCommendService tilCommendService;
    private final GithubCommitDetailService githubCommitDetailService;
    private final BlockingQueue<MapRecord<String, Object, Object>> processingQueue;

    @Async
    public void process(MapRecord<String, Object, Object> record) {
        Map<Object, Object> data = record.getValue();
        String requestId = (String) data.get("requestId");
        String userId = (String) data.get("userId");
        String requestJson = (String) data.get("requestJson");

        try {
            redisTemplate.opsForSet().add(ACTIVE_SET_KEY, requestId);
            if (redisTemplate.opsForSet().size(ACTIVE_SET_KEY) > 2) {
                log.info("동시성 초과 - 재삽입: {}", requestId);
                processingQueue.offer(record); // 🔁 다시 큐에 넣기
                Thread.sleep(1000); // busy loop 방지용 대기
                return;
            }

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
                            Long.parseLong(userId));

            TilAiResponseDTO aiResponse = tilAiService.generateTilContent(
                    commitDetail, request.getRepositoryId(), request.getBranch(),
                    request.getTitle());

            TilRequestDTO.CreateAiTilRequest saveRequest =
                    TilDtoConverter.toCreateAiTilRequest(request, aiResponse);

            TilResponseDTO.CreateTilResponse response =
                    tilCommendService.createTilFromAi(saveRequest, Long.parseLong(userId));

            redisTemplate.opsForValue().set("ai:til:result:" + requestId,
                    objectMapper.writeValueAsString(response), RESULT_TTL);

            redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP, record.getId());
            redisTemplate.opsForStream().delete(STREAM_KEY, record.getId());
            log.info("TIL 생성 완료: {}", requestId);
        } catch (Exception e) {
            log.error("TIL 처리 실패 - requestId={}, error={}", requestId, e.getMessage());
            redisTemplate.opsForValue()
                    .set("ai:til:result:" + requestId, "{\"error\":\"" + e.getMessage() + "\"}",
                            RESULT_TTL);
        } finally {
            redisTemplate.opsForSet().remove(ACTIVE_SET_KEY, requestId);
        }
    }
}

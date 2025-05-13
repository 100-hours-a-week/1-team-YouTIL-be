package com.youtil.Api.Tils.Service;

import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Tils.Converter.TilDtoConverter;
import com.youtil.Api.Tils.Dto.TilAiRequestDTO;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Exception.TilException.TilException.TilAIHealthxception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilAiService {

    private final RestTemplate restTemplate;
    private final WebClient webClient;
    @Value("${ai.api.url}")
    private String aiApiUrl;

    /**
     * 커밋 정보를 AI API로 전송하여 TIL 내용을 생성합니다.
     */
    public TilAiResponseDTO generateTilContent(
            CommitDetailResponseDTO.CommitDetailResponse commitDetail,
            Long repositoryId,
            String branch,
            String title) {
        log.info("AI API로 TIL 내용 생성 요청 - 제목: {}, 브랜치: {}, 파일 수: {}, AI 서버 URL: {}",
                title,
                branch,
                commitDetail.getFiles() != null ? commitDetail.getFiles().size() : 0,
                aiApiUrl);

        // 제목이 비어있는 경우 기본값 설정
        String finalTitle = (title != null && !title.isEmpty()) ? title : "커밋 기반 TIL";

        // 수정된 메서드 호출로 title 전달
        TilAiRequestDTO requestDTO = TilDtoConverter.toTilAiRequest(commitDetail, repositoryId,
                title);

        // 항상 title 필드 설정
        requestDTO.setTitle(finalTitle);

        // 요청 데이터 로깅 (제목 포함하도록 수정)
        log.info("AI 요청 데이터: 사용자={}, 레포지토리={}, 제목={}, 파일={}개",
                requestDTO.getUsername(),
                requestDTO.getRepo(),
                requestDTO.getTitle(),
                requestDTO.getFiles() != null ? requestDTO.getFiles().size() : 0);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // HTTP 요청 생성
        HttpEntity<TilAiRequestDTO> requestEntity = new HttpEntity<>(requestDTO, headers);

        String fullUrl = aiApiUrl + "/til";
        log.info("요청 전송 URL: {}", fullUrl);

        try {
            // AI API 호출
            ResponseEntity<TilAiResponseDTO> responseEntity = restTemplate.postForEntity(
                    fullUrl,
                    requestEntity,
                    TilAiResponseDTO.class);

            log.info("AI API 응답 수신 완료 - 상태 코드: {}", responseEntity.getStatusCode());

            if (responseEntity.getBody() == null) {
                log.error("AI 서버에서 빈 응답을 반환했습니다.");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI 서버에서 유효한 응답을 받지 못했습니다.");
            }

            log.info("AI 응답 내용: content 길이={}, tags={}",
                    responseEntity.getBody().getContent() != null ? responseEntity.getBody()
                            .getContent().length() : 0,
                    responseEntity.getBody().getKeywords());

            return responseEntity.getBody();

        } catch (RestClientException e) {
            log.error("AI API 호출 실패: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서버와의 연결이 원활하지 않습니다: " + e.getMessage());
        } catch (ResponseStatusException e) {
            // 이미 생성된 ResponseStatusException은 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("AI 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI 서비스 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * AI 서버 헬스 체크
     */
    public String getTilAIHealthService() {
        String fullUrl = aiApiUrl + "/health";
        try {
            return webClient.get()
                    .uri(fullUrl)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> Mono.error(new TilAIHealthxception())
                    )
                    .bodyToMono(String.class)
                    .block(); // 동기 호출
        } catch (Exception e) {
            throw new TilAIHealthxception();
        }
    }
}
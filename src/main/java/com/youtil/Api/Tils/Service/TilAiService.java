package com.youtil.Api.Tils.Service;

import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Tils.Converter.TilDtoConverter;
import com.youtil.Api.Tils.Dto.TilAiRequestDTO;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Exception.TilException.TilException.TilAIHealthxception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilAiService {


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

        // 제목이 비어있는 경우 기본값 설정
        String finalTitle = (title != null && !title.isEmpty()) ? title : "커밋 기반 TIL";

        // 수정된 메서드 호출로 title 전달
        TilAiRequestDTO requestDTO = TilDtoConverter.toTilAiRequest(commitDetail, repositoryId,
                title);

        requestDTO.setTitle(finalTitle);

        String fullUrl = aiApiUrl + "/til";

        try {
            // WebClient를 사용하여 AI API 호출 (RestTemplate 대체)
            TilAiResponseDTO response = webClient.post()
                    .uri(fullUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestDTO)
                    .retrieve()
                    .bodyToMono(TilAiResponseDTO.class)
                    .block();

            log.info("AI API 응답 수신 완료");

            if (response == null) {
                log.error("AI 서버에서 빈 응답을 반환했습니다.");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI 서버에서 유효한 응답을 받지 못했습니다.");
            }
            return response;

        } catch (WebClientResponseException e) {
            log.error("AI API 호출 실패: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서버와의 연결이 원활하지 않습니다: " + e.getMessage());
        } catch (ResponseStatusException e) {
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
    public String getTilAIHealthStatus() {
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

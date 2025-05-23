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

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilAiService {

    private final WebClient webClient;

    @Value("${ai.api.url.primary}")
    private String primaryAiApiUrl;

    @Value("${ai.api.url.secondary}")
    private String secondaryAiApiUrl;

    /**
     * 현재 시간에 따라 적절한 AI 서버 URL을 반환합니다.
     * 한국 시간(KST) 기준으로 판단합니다.
     * 오후 3시(15:00) ~ 오전 12시(24:00/00:00) : primary 서버 사용
     * 오전 12시(00:00) ~ 오후 3시(15:00) : secondary 서버 사용
     */
    private String getActiveAiServerUrl() {
        // 한국 시간대로 현재 시간 가져오기
        ZonedDateTime koreaTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime currentTime = koreaTime.toLocalTime();

        LocalTime afternoonThree = LocalTime.of(15, 0); // 오후 3시
        LocalTime midnight = LocalTime.of(0, 0); // 자정

        // 오후 3시부터 자정까지는 primary 서버 사용
        if (currentTime.isAfter(afternoonThree) || currentTime.equals(afternoonThree)) {
            log.debug("현재 한국 시간 {}로 primary AI 서버 사용: {}", currentTime, primaryAiApiUrl);
            return primaryAiApiUrl;
        }
        // 자정부터 오후 3시까지는 secondary 서버 사용
        else {
            log.debug("현재 한국 시간 {}로 secondary AI 서버 사용: {}", currentTime, secondaryAiApiUrl);
            return secondaryAiApiUrl;
        }
    }

    /**
     * 커밋 정보를 AI API로 전송하여 TIL 내용을 생성합니다.
     */
    public TilAiResponseDTO generateTilContent(
            CommitDetailResponseDTO.CommitDetailResponse commitDetail,
            Long repositoryId,
            String branch,
            String title) {


        // 현재 시간에 따른 AI 서버 URL 선택
        String currentAiApiUrl = getActiveAiServerUrl();

        log.info("AI API로 TIL 내용 생성 요청 [한국시간 기준] - 제목: {}, 브랜치: {}, 파일 수: {}, 사용 중인 AI 서버 URL: {}",
                title,
                branch,
                commitDetail.getFiles() != null ? commitDetail.getFiles().size() : 0,
                currentAiApiUrl);


        // 제목이 비어있는 경우 기본값 설정
        String finalTitle = (title != null && !title.isEmpty()) ? title : "커밋 기반 TIL";

        // 수정된 메서드 호출로 title 전달
        TilAiRequestDTO requestDTO = TilDtoConverter.toTilAiRequest(commitDetail, repositoryId,
                title);

        requestDTO.setTitle(finalTitle);


        // 요청 데이터 로깅 (제목 포함하도록 수정)
        log.info("AI 요청 데이터: 사용자={}, 레포지토리={}, 제목={}, 파일={}개",
                requestDTO.getUsername(),
                requestDTO.getRepo(),
                requestDTO.getTitle(),
                requestDTO.getFiles() != null ? requestDTO.getFiles().size() : 0);

        String fullUrl = currentAiApiUrl + "/til";
        log.info("요청 전송 URL: {}", fullUrl);


        try {
            // WebClient를 사용하여 AI API 호출 (RestTemplate 대체)
            TilAiResponseDTO response = webClient.post()
                    .uri(fullUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestDTO)
                    .retrieve()
                    .bodyToMono(TilAiResponseDTO.class)
                    .block();

            log.info("AI API 응답 수신 완료 (서버: {})", currentAiApiUrl);

            if (response == null) {
                log.error("AI 서버에서 빈 응답을 반환했습니다. (서버: {})", currentAiApiUrl);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI 서버에서 유효한 응답을 받지 못했습니다.");
            }

            log.info("AI 응답 내용: content 길이={}, tags={}",
                    response.getContent() != null ? response.getContent().length() : 0,
                    response.getKeywords());


            return response;

        } catch (WebClientResponseException e) {
            log.error("AI API 호출 실패 (서버: {}): {}", currentAiApiUrl, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서버와의 연결이 원활하지 않습니다: " + e.getMessage());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 처리 중 예상치 못한 오류 발생 (서버: {}): {}", currentAiApiUrl, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "AI 서비스 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * AI 서버 헬스 체크
     * 현재 활성화된 서버의 헬스를 체크합니다.
     */
    public String getTilAIHealthStatus() {
        String currentAiApiUrl = getActiveAiServerUrl();
        String fullUrl = currentAiApiUrl + "/health";

        log.info("AI 서버 헬스 체크 [한국시간 기준] (서버: {})", currentAiApiUrl);

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
            log.error("AI 서버 헬스 체크 실패 (서버: {}): {}", currentAiApiUrl, e.getMessage());
            throw new TilAIHealthxception();
        }
    }


}
package com.youtil.Api.Tils.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Tils.Converter.TilDtoConverter;
import com.youtil.Api.Tils.Dto.TilAiRequestDTO;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TilAiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    /**
     * 커밋 정보를 AI API로 전송하여 TIL 내용을 생성합니다.
     */
    public TilAiResponseDTO generateTilContent(CommitDetailResponseDTO.CommitDetailResponse commitDetail,
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
        TilAiRequestDTO requestDTO = TilDtoConverter.toTilAiRequest(commitDetail, repositoryId, title);

        // 항상 title 필드 설정
        requestDTO.setTitle(finalTitle);

        // 요청 데이터 로깅 (제목 포함하도록 수정)
        log.info("AI 요청 데이터: 사용자={}, 레포지토리={}, 제목={}, 파일={}개",
                requestDTO.getUsername(),
                requestDTO.getRepo(),
                requestDTO.getTitle(),
                requestDTO.getFiles() != null ? requestDTO.getFiles().size() : 0);

        // 파일 코드 길이 로깅
        if (requestDTO.getFiles() != null && !requestDTO.getFiles().isEmpty() &&
                commitDetail.getFiles() != null && !commitDetail.getFiles().isEmpty()) {

            String originalCode = commitDetail.getFiles().get(0).getLatest_code();
            String processedCode = requestDTO.getFiles().get(0).getLatest_code();

            log.info("첫 번째 파일 코드 길이 비교: 원본={}, 공백제거 후={}, 감소율={:.2f}%",
                    originalCode.length(),
                    processedCode.length(),
                    originalCode.length() > 0 ?
                            100.0 * (originalCode.length() - processedCode.length()) / originalCode.length() : 0);
        }

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // HTTP 요청 생성
        HttpEntity<TilAiRequestDTO> requestEntity = new HttpEntity<>(requestDTO, headers);

        String fullUrl = aiApiUrl + "/til";
        log.info("요청 전송 URL: {}", fullUrl);

        try {
            // String으로 응답 받기
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    fullUrl,
                    requestEntity,
                    String.class);

            log.info("AI API 응답 수신 완료 - 상태 코드: {}", responseEntity.getStatusCode());

            if (responseEntity.getBody() == null) {
                log.error("AI 서버에서 빈 응답을 반환했습니다.");
                return TilDtoConverter.createFallbackResponse(commitDetail);
            }

            String responseBody = responseEntity.getBody();
            log.debug("원본 응답: {}", responseBody);

            // 수동으로 TilAiResponseDTO 생성 및 파싱
            TilAiResponseDTO response = parseAiResponse(responseBody, commitDetail);

            log.info("파싱된 AI 응답: content 길이={}, keywords={}",
                    response.getContent() != null ? response.getContent().length() : 0,
                    response.getKeywords());

            return response;

        } catch (RestClientException e) {
            log.error("AI API 호출 실패: {}", e.getMessage(), e);
            log.info("폴백 응답 생성 중...");
            return TilDtoConverter.createFallbackResponse(commitDetail);
        } catch (Exception e) {
            log.error("AI 처리 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            log.info("폴백 응답 생성 중...");
            return TilDtoConverter.createFallbackResponse(commitDetail);
        }
    }

    /**
     * AI 응답을 파싱하여 TilAiResponseDTO 객체로 변환
     */
    private TilAiResponseDTO parseAiResponse(String responseBody, CommitDetailResponseDTO.CommitDetailResponse commitDetail) {
        try {
            // 먼저 표준 JSON 파싱 시도
            try {
                return objectMapper.readValue(responseBody, TilAiResponseDTO.class);
            } catch (Exception e) {
                log.warn("표준 JSON 파싱 실패, 커스텀 파싱 시도: {}", e.getMessage());
            }

            // 커스텀 파싱
            TilAiResponseDTO response = new TilAiResponseDTO();

            // 1. content 필드 추출
            if (responseBody.contains("\"content\"")) {
                Pattern contentPattern = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"(,|\\})", Pattern.DOTALL);
                Matcher contentMatcher = contentPattern.matcher(responseBody);
                if (contentMatcher.find()) {
                    String content = contentMatcher.group(1);
                    // JSON 이스케이프 문자 처리
                    content = content.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
                    response.setContent(content);
                }
            }

            // 2. keywords 필드 추출 및 파싱
            List<String> keywords = new ArrayList<>();
            if (responseBody.contains("\"keywords\"")) {
                // 문자열에서 keywords 부분 추출
                Pattern keywordsPattern = Pattern.compile("\"keywords\"\\s*:\\s*(.*?)(,\\s*\"|\\})", Pattern.DOTALL);
                Matcher keywordsMatcher = keywordsPattern.matcher(responseBody);

                if (keywordsMatcher.find()) {
                    String keywordsStr = keywordsMatcher.group(1).trim();

                    // JSON 배열 패턴 찾기
                    Pattern arrayPattern = Pattern.compile("\\[\\s*\"([^\"]+)\"(?:\\s*,\\s*\"([^\"]+)\")*\\s*\\]");
                    Matcher arrayMatcher = arrayPattern.matcher(keywordsStr);

                    if (arrayMatcher.find()) {
                        // 배열 형태로 파싱 시도
                        try {
                            String arrayStr = arrayMatcher.group(0);
                            keywords = objectMapper.readValue(arrayStr, new TypeReference<List<String>>() {});
                        } catch (Exception e) {
                            log.warn("keywords 배열 파싱 실패: {}", e.getMessage());
                        }
                    } else {
                        // 문자열 내의 키워드 추출 시도
                        Pattern quotePattern = Pattern.compile("\"([^\"]+)\"");
                        Matcher quoteMatcher = quotePattern.matcher(keywordsStr);
                        while (quoteMatcher.find()) {
                            keywords.add(quoteMatcher.group(1));
                        }
                    }
                }
            }

            // 키워드가 없으면 기본 키워드 사용
            if (keywords.isEmpty()) {
                keywords = Arrays.asList("TIL", "개발", "커밋요약");
            }

            response.setKeywords(keywords);

            // 컨텐츠가 없으면 폴백 응답 사용
            if (response.getContent() == null || response.getContent().isEmpty()) {
                return TilDtoConverter.createFallbackResponse(commitDetail);
            }

            return response;

        } catch (Exception e) {
            log.error("AI 응답 파싱 중 오류 발생: {}", e.getMessage(), e);
            return TilDtoConverter.createFallbackResponse(commitDetail);
        }
    }
}
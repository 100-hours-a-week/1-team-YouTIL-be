package com.youtil.Api.News.Service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class TranslationService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://translation.googleapis.com/language/translate/v2")
            .build();

    @Value("${google.api-key}")
    private String apiKey;

    public String translateText(String text, String targetLang) {
    if (text == null || text.trim().isEmpty()) return "";


    String cleanedText = sanitizeText(text);
    Map<String, Object> requestBody = Map.of(
        "q", cleanedText,
        "target", targetLang,
        "format", "text"
    );

    JsonNode response = webClient.post()
            .uri(uriBuilder -> uriBuilder.queryParam("key", apiKey).build())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    return response
            .path("data")
            .path("translations")
            .get(0)
            .path("translatedText")
            .asText();
}

    // JSON 이스케이프를 위한 유틸 (큰따옴표 등 처리)
    private String escapeJson(String raw) {
        return raw.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    private static String sanitizeText(String input) {
    if (input == null) return "";
    return input
            .replaceAll("[\\u2013\\u2014]", "-") // EN DASH, EM DASH → 일반 하이픈으로 교체
            .replaceAll("[\\p{C}]", "")         // 제어문자 제거
            .trim();
}
}



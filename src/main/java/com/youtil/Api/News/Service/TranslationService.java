package com.youtil.Api.News.Service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@RequiredArgsConstructor
public class TranslationService {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://translation.googleapis.com/language/translate/v2")
            .build();

    @Value("${google.api-key}")
    private String apiKey;

    public String translateText(String text, String targetLang) {
        JsonNode response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("key", apiKey)
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(String.format(
                        "{\"q\":\"%s\", \"target\":\"%s\", \"format\":\"text\"}",
                        escapeJson(text), targetLang))
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
}



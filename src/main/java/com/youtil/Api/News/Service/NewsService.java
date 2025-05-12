package com.youtil.Api.News.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.youtil.Api.News.Converter.NewsConverter;
import com.youtil.Api.News.Dto.NewsResponseDTO;
import com.youtil.Api.News.Dto.NewsResponseDTO.NewsItem;
import com.youtil.Config.AppProperties;
import com.youtil.Model.News;
import com.youtil.Repository.NewsRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service

@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final TranslationService translationService;
    private final AppProperties appProperties;
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://newsdata.io/api/1")
            .build();
    @Value("${news.key}")
    private String API_KEY;

    public NewsResponseDTO.GetNewsResponse getNewsService() {
        String baseUrl = appProperties.getServerDomain();
        List<News> newsList = newsRepository.findAllByOrderByCreatedAtDesc();
        List<NewsItem> newsItemList = newsList.stream().map(news -> {
            return NewsConverter.toNewsItem(news, baseUrl);
        }).toList();

        return NewsConverter.toGetNewsResponse(newsItemList);
    }

    @Transactional
    public void createNewsService() {
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/news")
                        .queryParam("apikey", API_KEY)
                        .queryParam("q", "developer ai cloud server")
                        .queryParam("language", "ko,en")
                        .queryParam("category", "technology")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        JsonNode results = response.path("results");

        if (results != null && results.isArray()) {
            for (JsonNode result : results) {
                if (!result.path("duplicate").asBoolean(false)) {
                    String originUrl = result.path("link").asText(null);
                    if (!newsRepository.existsByOriginUrl(originUrl)) {
                        OffsetDateTime pubDate = parsePubDate(result.path("pubDate").asText(null));
                        String originalTitle = result.path("title").asText(null);
                        //mvp 1차에서는 타이틀만 번역
                        String translatedTitle = translationService.translateText(originalTitle,
                                "ko");

                        News news = News.builder()
                                .title(translatedTitle)
                                .originUrl(originUrl)
                                .content(result.path("description").asText("요약본 미제공"))
                                .thumbnail(sanitizeImageUrl(result.path("image_url").asText(null)))
                                .createdAt(pubDate)
                                .build();

                        newsRepository.save(news);
                    }
                }
            }
        }

        long count = newsRepository.count();
        if (count > 10) {
            List<News> toDelete = newsRepository.findAll(
                    PageRequest.of(0, (int) (count - 10), Sort.by(Sort.Direction.ASC, "createdAt"))
            ).getContent();

            newsRepository.deleteAll(toDelete);
        }
    }

    private OffsetDateTime parsePubDate(String raw) {
        try {
            return OffsetDateTime.parse(raw,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(
                            ZoneId.of("UTC")));
        } catch (Exception e) {
            return OffsetDateTime.now(ZoneId.of("Asia/Seoul")); // fallback
        }
    }

    private String sanitizeImageUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        // 1. 공백, 제어문자 제거
        String cleanedUrl = rawUrl.trim().replaceAll("[\\t\\n\\r]", "");

        // 2. 두 번째 https:// 또는 http:// 이후부터 자르기
        int secondIndex = -1;
        if (cleanedUrl.indexOf("https://", 8) != -1) {
            secondIndex = cleanedUrl.indexOf("https://", 8);
        } else if (cleanedUrl.indexOf("http://", 7) != -1) {
            secondIndex = cleanedUrl.indexOf("http://", 7);
        }

        if (secondIndex != -1) {
            cleanedUrl = cleanedUrl.substring(secondIndex);
        }

        // 3. http:/// 또는 https:/// 정규화
        cleanedUrl = cleanedUrl.replaceAll("^(https?:)/{3,}", "$1//");

        // 4. URI 검증
        try {
            URI uri = new URI(cleanedUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return uri.toString();
        } catch (Exception e) {
            return null;
        }
    }

}

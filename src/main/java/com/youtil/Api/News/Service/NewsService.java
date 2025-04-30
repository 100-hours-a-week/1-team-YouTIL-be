package com.youtil.Api.News.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.youtil.Api.News.Converter.NewsConverter;
import com.youtil.Api.News.Dto.NewsResponseDTO;
import com.youtil.Api.News.Dto.NewsResponseDTO.NewsItem;
import com.youtil.Model.News;
import com.youtil.Repository.NewsRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@AllArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://newsdata.io/api/1")
            .build();


    private final String API_KEY = "YOUR_API_KEY";

    public NewsResponseDTO.GetNewsResponse getNewsService() {
        List<News> newsList = newsRepository.findAllByOrderByCreatedAtDesc();
        List<NewsItem> newsItemList = newsList.stream().map(news -> {
            return NewsConverter.toNewsItem(news);
        }).toList();

        return NewsConverter.toGetNewsResponse(newsItemList);
    }

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

        List<NewsItem> newsItems = new ArrayList<>();

        if (results != null && results.isArray()) {
            for (JsonNode result : results) {
                if (!result.path("duplicate").asBoolean(false)) {

                    News news = News.builder()
                            .title(result.path("title").asText(null))
                            .originUrl(result.path("link").asText(null))
                            .content(result.path("description").asText(null))
                            .thumbnail(result.path("image_url").asText(null))
                            .build();

                    // 중복 저장 방지를 위해 originUrl 기준 검사
//                    if (!newsRepository.existsByOriginUrl(news.getOriginUrl())) {
//                        newsRepository.save(news);
//                        newsItems.add(NewsConverter.toNewsItem(news));
//                    }
                }
            }
        }

    }

}

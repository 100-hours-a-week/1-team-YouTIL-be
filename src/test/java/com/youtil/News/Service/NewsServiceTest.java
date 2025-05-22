package com.youtil.News.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.youtil.Api.News.Dto.NewsResponseDTO.GetNewsResponse;
import com.youtil.Api.News.Service.NewsService;
import com.youtil.Api.News.Service.TranslationService;
import com.youtil.Config.AppProperties;
import com.youtil.Model.News;
import com.youtil.Repository.NewsRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.assertj.core.util.Lists;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@ExtendWith(MockitoExtension.class)
public class NewsServiceTest {

    private final String BASE_URL = "https://mock.domain.com";
    @Mock
    private NewsRepository newsRepository;
    @InjectMocks
    private NewsService newsService;
    @Mock
    private WebClient webClient;
    @Mock
    private AppProperties appProperties;
    @Mock
    private TranslationService translationService;
    private News mockNews;
    private WebClient.RequestHeadersUriSpec getUriSpec;
    private WebClient.RequestHeadersSpec getHeaderSpec;
    private WebClient.ResponseSpec getResponseSpec;

    News createNews() {
        News news = News.builder()
                .id(1L)
                .title("title")
                .content("content")
                .originUrl("originUrl")
                .thumbnail("thumbnail")
                .createdAt(OffsetDateTime.now())
                .build();
        return news;
    }


    //JsonNode 모킹
    private void setupMockNewsJsonNode(JsonNode mockResponse, JsonNode mockResults,
            JsonNode mockResultItem,
            String url, String pubDate) {
        JsonNode duplicateNode = mock(JsonNode.class);
        JsonNode linkNode = mock(JsonNode.class);
        JsonNode pubDateNode = mock(JsonNode.class);
        JsonNode titleNode = mock(JsonNode.class);
        JsonNode descriptionNode = mock(JsonNode.class);
        JsonNode imageUrlNode = mock(JsonNode.class);

        when(mockResponse.path("results")).thenReturn(mockResults);
        when(mockResults.isArray()).thenReturn(true);
        when(mockResults.iterator()).thenReturn(List.of(mockResultItem).iterator());

        when(mockResultItem.path("duplicate")).thenReturn(duplicateNode);
        when(duplicateNode.asBoolean(false)).thenReturn(false);

        when(mockResultItem.path("link")).thenReturn(linkNode);
        when(linkNode.asText(null)).thenReturn(url);
        when(newsRepository.existsByOriginUrl(url)).thenReturn(false);

        when(mockResultItem.path("pubDate")).thenReturn(pubDateNode);
        when(pubDateNode.asText(null)).thenReturn(pubDate);

        when(mockResultItem.path("title")).thenReturn(titleNode);
        when(titleNode.asText(null)).thenReturn("Original title");
        when(translationService.translateText("Original title", "ko")).thenReturn("번역된 제목");

        when(mockResultItem.path("description")).thenReturn(descriptionNode);
        when(descriptionNode.asText("요약본 미제공")).thenReturn("요약 내용");

        when(mockResultItem.path("image_url")).thenReturn(imageUrlNode);
        when(imageUrlNode.asText(null)).thenReturn("https://example.com/image.jpg");
    }

    //웹클라이언트 모킹
    private void setupWebClientMock(JsonNode mockResponse) {
        getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);

        getResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get())
                .thenReturn(getUriSpec);

        when(getUriSpec.uri(any(Function.class))).thenReturn(getHeaderSpec);
        when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(JsonNode.class))).thenReturn(Mono.just(mockResponse));
    }

    @BeforeEach()
    void setUp() {
        mockNews = createNews();
    }

    @Test
    @DisplayName("뉴스 조회 - 뉴스가 있을 경우 - 뉴스 조회 성공")
    void getNews_withValidNews_success() {
        when(appProperties.getServerDomain()).thenReturn(BASE_URL);
        List<News> tempList = Lists.newArrayList(mockNews, mockNews);
        when(newsRepository.findAllByOrderByCreatedAtDesc()).thenReturn(tempList);

        GetNewsResponse getNewsResponse = newsService.getNewsService();
        assertEquals(tempList.get(0).getTitle(), getNewsResponse.getNews().get(0).getTitle());

    }

    @Test
    @DisplayName("뉴스 조회 - 뉴스가 없을 경우 - 뉴스 조회 성공")
    void getNews_withInvalidNews_success() {
        when(appProperties.getServerDomain()).thenReturn(BASE_URL);
        when(newsRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        GetNewsResponse getNewsResponse = newsService.getNewsService();
        assertNotNull(getNewsResponse);
        assertTrue(getNewsResponse.getNews().isEmpty());
    }

    //뉴스 생성
    @Test
    @DisplayName("뉴스 생성 - 10개 미만 - 성공")
    void createNews_withDataMaxThanTen_success() {
        JsonNode mockResponse = mock(JsonNode.class);
        JsonNode mockResults = mock(JsonNode.class);
        JsonNode mockResultItem = mock(JsonNode.class);

        when(newsRepository.count()).thenReturn(4L);
        setupWebClientMock(mockResponse);
        setupMockNewsJsonNode(mockResponse, mockResults, mockResultItem,
                "http://example.com/news", "2024-04-01 10:00:00");

        newsService.createNewsService();

        verify(newsRepository, times(1)).save(any(News.class));
        verify(newsRepository, never()).deleteAll(any());
    }


    @Test
    @DisplayName("뉴스 생성 - 저장되있는 데이터가 10개 이상일 경우 - 뉴스 생성 성공")
    void createNews_withDataMinThanTen_success() {
        JsonNode mockResponse = mock(JsonNode.class);
        JsonNode mockResults = mock(JsonNode.class);
        JsonNode mockResultItem = mock(JsonNode.class);

        when(newsRepository.count()).thenReturn(11L);
        setupWebClientMock(mockResponse);
        setupMockNewsJsonNode(mockResponse, mockResults, mockResultItem,
                "https://example.com/news", "2024-04-01 10:00:00");

        News oldNews = News.builder()
                .id(30L)
                .title("old")
                .originUrl("http://example.com/old")
                .createdAt(OffsetDateTime.parse("2023-01-01T00:00:00+00:00"))
                .build();
        when(newsRepository.findAll(
                PageRequest.of(0, 1, Sort.by(Direction.ASC, "createdAt")))
        ).thenReturn(new PageImpl<>(List.of(oldNews)));

        newsService.createNewsService();

        verify(newsRepository, times(1)).save(any(News.class));
        verify(newsRepository, times(1)).deleteAll(
                argThat(iterable -> {
                    List<News> newsList = StreamSupport.stream(iterable.spliterator(), false)
                            .collect(Collectors.toList());
                    return newsList.contains(oldNews) && newsList.size() == 1;
                })
        );

    }

}

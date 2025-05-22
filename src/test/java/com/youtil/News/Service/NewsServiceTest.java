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

    private void setupWebClient() {
        // GET - Email 정보와 유저 정보
        getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);

        getResponseSpec = mock(WebClient.ResponseSpec.class);

        //처음 get을 호출하면 이메일, 두번째 호출하면 user 정보로 규정한다.
        when(webClient.get())
                .thenReturn(getUriSpec);
    }


    @BeforeEach()
    void setUp() {
        mockNews = createNews();

    }

    @Test
    @DisplayName("뉴스 조회 - 뉴스가 있을 경우 - 뉴스 조회 성공")
    void getNews_withValidNews_success() {
        when(appProperties.getServerDomain()).thenReturn("https://mock.domain.com");
        List<News> tempList = Lists.newArrayList(mockNews, mockNews);
        when(newsRepository.findAllByOrderByCreatedAtDesc()).thenReturn(tempList);

        GetNewsResponse getNewsResponse = newsService.getNewsService();
        assertEquals(tempList.get(0).getTitle(), getNewsResponse.getNews().get(0).getTitle());

    }

    @Test
    @DisplayName("뉴스 조회 - 뉴스가 없을 경우 - 뉴스 조회 성공")
    void getNews_withInvalidNews_success() {
        when(appProperties.getServerDomain()).thenReturn("https://mock.domain.com");
        when(newsRepository.findAllByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        GetNewsResponse getNewsResponse = newsService.getNewsService();
        assertNotNull(getNewsResponse);
        assertTrue(getNewsResponse.getNews().isEmpty());
    }

    //뉴스 생성
    @Test
    @DisplayName("뉴스 생성 - 저장되어 있는 데이터가 10개 미만인 경우 - 뉴스 생성 성공")
    void createNews_withDataMaxThanTen_success() {
        // given
        JsonNode mockResponse = mock(JsonNode.class);
        JsonNode mockResults = mock(JsonNode.class);
        JsonNode mockResultItem = mock(JsonNode.class);

        // 각 필드에 대한 JsonNode mock
        JsonNode duplicateNode = mock(JsonNode.class);
        JsonNode linkNode = mock(JsonNode.class);
        JsonNode pubDateNode = mock(JsonNode.class);
        JsonNode titleNode = mock(JsonNode.class);
        JsonNode descriptionNode = mock(JsonNode.class);
        JsonNode imageUrlNode = mock(JsonNode.class);

        // 저장된 뉴스는 4개
        when(newsRepository.count()).thenReturn(4L);

        //  mock 체인 구성
        setupWebClient();
        when(getUriSpec.uri(any(Function.class))).thenReturn(getHeaderSpec);
        when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(JsonNode.class))).thenReturn(Mono.just(mockResponse));

        // mockResultItem 하나만 있다고 가정
        when(mockResponse.path("results")).thenReturn(mockResults);
        when(mockResults.isArray()).thenReturn(true);
        when(mockResults.iterator()).thenReturn(List.of(mockResultItem).iterator());

        //뉴스 데이터 및  번역 Mock Chaining
        when(mockResultItem.path("duplicate")).thenReturn(duplicateNode);
        when(duplicateNode.asBoolean(false)).thenReturn(false);

        when(mockResultItem.path("link")).thenReturn(linkNode);
        when(linkNode.asText(null)).thenReturn("http://example.com/news");

        when(newsRepository.existsByOriginUrl("http://example.com/news")).thenReturn(false);

        when(mockResultItem.path("pubDate")).thenReturn(pubDateNode);
        when(pubDateNode.asText(null)).thenReturn("2024-04-01 10:00:00");

        when(mockResultItem.path("title")).thenReturn(titleNode);
        when(titleNode.asText(null)).thenReturn("Original title");

        when(translationService.translateText("Original title", "ko")).thenReturn("번역된 제목");

        when(mockResultItem.path("description")).thenReturn(descriptionNode);
        when(descriptionNode.asText("요약본 미제공")).thenReturn("요약 내용");

        when(mockResultItem.path("image_url")).thenReturn(imageUrlNode);
        when(imageUrlNode.asText(null)).thenReturn("https://example.com/image.jpg");

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

        JsonNode duplicateNode = mock(JsonNode.class);
        JsonNode linkNode = mock(JsonNode.class);
        JsonNode pubDateNode = mock(JsonNode.class);
        JsonNode titleNode = mock(JsonNode.class);
        JsonNode descriptionNode = mock(JsonNode.class);
        JsonNode imageUrlNode = mock(JsonNode.class);

        when(newsRepository.count()).thenReturn(11L);

        setupWebClient();
        when(getUriSpec.uri(any(Function.class))).thenReturn(getHeaderSpec);
        when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(JsonNode.class))).thenReturn(Mono.just(mockResponse));

        when(mockResponse.path("results")).thenReturn(mockResults);
        when(mockResults.isArray()).thenReturn(true);
        when(mockResults.iterator()).thenReturn(List.of(mockResultItem).iterator());

        when(mockResultItem.path("duplicate")).thenReturn(duplicateNode);
        when(duplicateNode.asBoolean(false)).thenReturn(false);
        when(mockResultItem.path("link")).thenReturn(linkNode);
        when(linkNode.asText(null)).thenReturn("https://example.com/news");
        when(newsRepository.existsByOriginUrl("https://example.com/news")).thenReturn(false);
        when(mockResultItem.path("pubDate")).thenReturn(pubDateNode);
        when(pubDateNode.asText(null)).thenReturn("2024-04-01 10:00:00");
        when(mockResultItem.path("title")).thenReturn(titleNode);
        when(titleNode.asText(null)).thenReturn("Original title");
        when(translationService.translateText("Original title", "ko")).thenReturn("번역된 제목");

        when(mockResultItem.path("description")).thenReturn(descriptionNode);
        when(descriptionNode.asText("요약본 미제공")).thenReturn("요약 내용");

        when(mockResultItem.path("image_url")).thenReturn(imageUrlNode);
        when(imageUrlNode.asText(null)).thenReturn("https://example.com/image.jpg");

        News oldNews = News.builder()
                .id(30L)
                .title("old")
                .originUrl("http://example.com/old")
                .createdAt(OffsetDateTime.parse("2023-01-01T00:00:00+00:00"))
                .build();
        when(newsRepository.findAll(
                PageRequest.of(0, (int) (newsRepository.count() - 10),
                        Sort.by(Direction.ASC, "createdAt")))
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

package com.youtil.News.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.youtil.Api.News.Dto.NewsResponseDTO.GetNewsResponse;
import com.youtil.Api.News.Dto.NewsResponseDTO.NewsItem;
import com.youtil.Api.News.Service.NewsService;
import com.youtil.Api.News.Service.TranslationService;
import com.youtil.Config.AppProperties;
import static com.youtil.Constants.NewsServiceConstants.BASE_URL;
import static com.youtil.Constants.NewsServiceConstants.DEFAULT_DESCRIPTION;
import static com.youtil.Constants.NewsServiceConstants.DEFAULT_IMAGE_URL;
import static com.youtil.Constants.NewsServiceConstants.FALLBACK_DESCRIPTION;
import static com.youtil.Constants.NewsServiceConstants.MOCK_NEWS_URL;
import static com.youtil.Constants.NewsServiceConstants.MOCK_PUB_DATE;
import static com.youtil.Constants.NewsServiceConstants.ORIGINAL_TITLE;
import static com.youtil.Constants.NewsServiceConstants.PATH_DESCRIPTION;
import static com.youtil.Constants.NewsServiceConstants.PATH_DUPLICATE;
import static com.youtil.Constants.NewsServiceConstants.PATH_IMAGE_URL;
import static com.youtil.Constants.NewsServiceConstants.PATH_LINK;
import static com.youtil.Constants.NewsServiceConstants.PATH_PUB_DATE;
import static com.youtil.Constants.NewsServiceConstants.PATH_RESULTS;
import static com.youtil.Constants.NewsServiceConstants.PATH_TITLE;
import static com.youtil.Constants.NewsServiceConstants.TARGET_LANG;
import static com.youtil.Constants.NewsServiceConstants.TRANSLATED_TITLE;
import static com.youtil.Mock.MockNewsBuilder.createNews;
import com.youtil.Model.News;
import com.youtil.Repository.NewsRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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


    //JsonNode 모킹
    private void setupMockNewsJsonNode(JsonNode mockResponse, JsonNode mockResults,
            JsonNode mockResultItem, String url, String pubDate) {

        JsonNode duplicateNode = mock(JsonNode.class);
        JsonNode linkNode = mock(JsonNode.class);
        JsonNode pubDateNode = mock(JsonNode.class);
        JsonNode titleNode = mock(JsonNode.class);
        JsonNode descriptionNode = mock(JsonNode.class);
        JsonNode imageUrlNode = mock(JsonNode.class);

        when(mockResponse.path(PATH_RESULTS)).thenReturn(mockResults);
        when(mockResults.isArray()).thenReturn(true);
        when(mockResults.iterator()).thenReturn(List.of(mockResultItem).iterator());

        when(mockResultItem.path(PATH_DUPLICATE)).thenReturn(duplicateNode);
        when(duplicateNode.asBoolean(false)).thenReturn(false);

        when(mockResultItem.path(PATH_LINK)).thenReturn(linkNode);
        when(linkNode.asText(null)).thenReturn(url);
        when(newsRepository.existsByOriginUrl(url)).thenReturn(false);

        when(mockResultItem.path(PATH_PUB_DATE)).thenReturn(pubDateNode);
        when(pubDateNode.asText(null)).thenReturn(pubDate);

        when(mockResultItem.path(PATH_TITLE)).thenReturn(titleNode);
        when(titleNode.asText(null)).thenReturn(ORIGINAL_TITLE);
        when(translationService.translateText(ORIGINAL_TITLE, TARGET_LANG)).thenReturn(
                TRANSLATED_TITLE);

        when(mockResultItem.path(PATH_DESCRIPTION)).thenReturn(descriptionNode);
        when(descriptionNode.asText(FALLBACK_DESCRIPTION)).thenReturn(DEFAULT_DESCRIPTION);

        when(mockResultItem.path(PATH_IMAGE_URL)).thenReturn(imageUrlNode);
        when(imageUrlNode.asText(null)).thenReturn(DEFAULT_IMAGE_URL);
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
        final String PROXY_URL = BASE_URL + "/api/v1/news/image-proxy?url=";

        when(appProperties.getServerDomain()).thenReturn(BASE_URL);

        List<News> newsList = List.of(mockNews, mockNews);
        when(newsRepository.findAllByOrderByCreatedAtDesc()).thenReturn(newsList);

        GetNewsResponse response = newsService.getNewsService();

        List<NewsItem> result = response.getNews();
        assertEquals(newsList.size(), result.size());

        for (int i = 0; i < newsList.size(); i++) {
            News source = newsList.get(i);
            NewsItem target = result.get(i);

            assertEquals(source.getTitle(), target.getTitle());
            assertEquals(source.getContent(), target.getSummary());
            assertEquals(source.getOriginUrl(), target.getLink());
            assertEquals(PROXY_URL + source.getThumbnail(), target.getThumbnail());
        }


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
        final long NEWS_COUNT = 4L;
        JsonNode mockResponse = mock(JsonNode.class);
        JsonNode mockResults = mock(JsonNode.class);
        JsonNode mockResultItem = mock(JsonNode.class);

        when(newsRepository.count()).thenReturn(NEWS_COUNT);
        setupWebClientMock(mockResponse);
        setupMockNewsJsonNode(mockResponse, mockResults, mockResultItem,
                MOCK_NEWS_URL, MOCK_PUB_DATE);

        newsService.createNewsService();

        verify(newsRepository, times(1)).save(any(News.class));
        verify(newsRepository, never()).deleteAll(any());
    }


    @Test
    @DisplayName("뉴스 생성 - 저장되있는 데이터가 10개 이상일 경우 - 뉴스 생성 성공")
    void createNews_withDataMinThanTen_success() {
        final long NEWS_COUNT = 11L;
        final long OLD_NEWS_ID = 30L;
        final String OLD_NEWS_TITLE = "old";
        final String OLD_NEWS_URL = "http://example.com/old";
        final String OLD_NEWS_PUB_DATE = "2023-01-01T00:00:00+00:00";
        final int pageNumber = 0;
        final int pageSize = 1;
        final String properties = "createdAt";

        JsonNode mockResponse = mock(JsonNode.class);
        JsonNode mockResults = mock(JsonNode.class);
        JsonNode mockResultItem = mock(JsonNode.class);

        when(newsRepository.count()).thenReturn(NEWS_COUNT);
        setupWebClientMock(mockResponse);
        setupMockNewsJsonNode(mockResponse, mockResults, mockResultItem,
                MOCK_NEWS_URL, MOCK_PUB_DATE);

        News oldNews = News.builder()
                .id(OLD_NEWS_ID)
                .title(OLD_NEWS_TITLE)
                .originUrl(OLD_NEWS_URL)
                .createdAt(OffsetDateTime.parse(OLD_NEWS_PUB_DATE))
                .build();
        when(newsRepository.findAll(
                PageRequest.of(pageNumber, pageSize, Sort.by(Direction.ASC, properties)))
        ).thenReturn(new PageImpl<>(List.of(oldNews)));

        newsService.createNewsService();

        verify(newsRepository, times(1)).save(any(News.class));
        verify(newsRepository, times(1)).deleteAll(
                argThat(iterable -> {
                    List<News> newsList = StreamSupport.stream(iterable.spliterator(), false)
                            .collect(Collectors.toList());
                    return newsList.contains(oldNews) && newsList.size() == pageSize;
                })
        );

    }

}

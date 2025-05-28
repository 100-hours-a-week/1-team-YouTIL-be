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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
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
        final long NEW_NEWS_COUNT = 2L;

        JsonNode mockResponse = mock(JsonNode.class);
        JsonNode mockResults = mock(JsonNode.class);
        when(newsRepository.count()).thenReturn(NEWS_COUNT);
        setupWebClientMock(mockResponse);
        setupMockNewsJsonNode(mockResponse, mockResults, NEW_NEWS_COUNT);

        newsService.createNewsService();

        verify(newsRepository, times((int) NEW_NEWS_COUNT)).save(any(News.class));
        verify(newsRepository, never()).deleteAll(any());
    }


    @Test
    @DisplayName("뉴스 생성 - 저장되있는 데이터가 10개 이상일 경우 - 뉴스 생성 성공")
    void createNews_withDataMinThanTen_success() {
        final long CURRENT_NEWS_COUNT = 10L;
        final long ADD_NEWS_COUNT = 2L;
        final long OLD_NEWS_ID = 30L;
        final String OLD_NEWS_TITLE = "old";
        final String OLD_NEWS_URL = "http://example.com/old";

        final String OLD_NEWS_PUB_DATE = "2023-01-01T00:00:00+00:00";

        int pageNumber = 0;
        //페이지 사이즈는 기존 뉴스 데이터들의 개수와, 앞으로 추가될 뉴스 데이터의 개수를 더한 뒤 10개보다 초과된 데이터의 개수를 저장한다.
        int pageSize = (int) (CURRENT_NEWS_COUNT + ADD_NEWS_COUNT - 10);
        final String properties = "createdAt";

        JsonNode mockResponse = mock(JsonNode.class);
        JsonNode mockResults = mock(JsonNode.class);

        //초기 모킹

        //본 로직은 마지막에 데이터가 10개 넘는다면 삭제하는 로직을 추가한다. 따라서 기존 뉴스데이터와 추가 뉴스데이터 개수를 더해서 리턴한다.
        when(newsRepository.count()).thenReturn(CURRENT_NEWS_COUNT + ADD_NEWS_COUNT);

        setupWebClientMock(mockResponse);
        setupMockNewsJsonNode(mockResponse, mockResults, ADD_NEWS_COUNT);

        //삭제할 오래도니 뉴스 리스트
        List<News> oldNewsList = LongStream.range(0, pageSize)
                .mapToObj(i -> News.builder()
                        .id(OLD_NEWS_ID + i)
                        .title(OLD_NEWS_TITLE + i)
                        .originUrl(OLD_NEWS_URL + "/" + i)
                        .createdAt(OffsetDateTime.parse(OLD_NEWS_PUB_DATE).minusDays(i))
                        .build())
                .collect(Collectors.toList());

        when(newsRepository.findAll(
                PageRequest.of(pageNumber, pageSize, Sort.by(Direction.ASC, properties)))
        ).thenReturn(new PageImpl<>(oldNewsList));

        newsService.createNewsService();

        verify(newsRepository, times(pageSize)).save(any(News.class));
        //리스트를 삭제하기떄문에 딱 한번 호출되야함 더불어서 삭제되는 리스트가 의도한 리스트가 맞는지 확인
        verify(newsRepository, times(1)).deleteAll(argThat(iterable ->
                StreamSupport.stream(iterable.spliterator(), false)
                        .toList()
                        .equals(oldNewsList)
        ));

    }

    //JsonNode 모킹
    private void setupMockNewsJsonNode(JsonNode mockResponse, JsonNode mockResults,
            long newsSize) {

        List<String> urls = new ArrayList<>();
        List<String> pubDates = new ArrayList();
        List<JsonNode> mockResultItems = IntStream.range(0, (int) newsSize)
                .mapToObj(i -> mock(JsonNode.class))
                .collect(Collectors.toList());

        for (int i = 0; i < newsSize; i++) {
            urls.add(mockNews.getOriginUrl() + i);
            pubDates.add(mockNews.getCreatedAt().toString());
        }

        when(mockResponse.path(PATH_RESULTS)).thenReturn(mockResults);
        when(mockResults.isArray()).thenReturn(true);
        when(mockResults.iterator()).thenReturn(mockResultItems.iterator());

        for (int i = 0; i < mockResultItems.size(); i++) {
            JsonNode item = mockResultItems.get(i);
            String url = urls.get(i);
            String pubDate = pubDates.get(i);

            JsonNode duplicateNode = mock(JsonNode.class);
            JsonNode linkNode = mock(JsonNode.class);
            JsonNode pubDateNode = mock(JsonNode.class);
            JsonNode titleNode = mock(JsonNode.class);
            JsonNode descriptionNode = mock(JsonNode.class);
            JsonNode imageUrlNode = mock(JsonNode.class);

            when(item.path(PATH_DUPLICATE)).thenReturn(duplicateNode);
            when(duplicateNode.asBoolean(false)).thenReturn(false);

            when(item.path(PATH_LINK)).thenReturn(linkNode);
            when(linkNode.asText(null)).thenReturn(url);
            when(newsRepository.existsByOriginUrl(url)).thenReturn(false);

            when(item.path(PATH_PUB_DATE)).thenReturn(pubDateNode);
            when(pubDateNode.asText(null)).thenReturn(pubDate);

            when(item.path(PATH_TITLE)).thenReturn(titleNode);
            when(titleNode.asText(null)).thenReturn(ORIGINAL_TITLE);
            when(translationService.translateText(ORIGINAL_TITLE, TARGET_LANG))
                    .thenReturn(TRANSLATED_TITLE);

            when(item.path(PATH_DESCRIPTION)).thenReturn(descriptionNode);
            when(descriptionNode.asText(FALLBACK_DESCRIPTION)).thenReturn(DEFAULT_DESCRIPTION);

            when(item.path(PATH_IMAGE_URL)).thenReturn(imageUrlNode);
            when(imageUrlNode.asText(null)).thenReturn(DEFAULT_IMAGE_URL);
        }
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

}

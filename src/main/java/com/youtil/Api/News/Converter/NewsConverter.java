package com.youtil.Api.News.Converter;

import com.youtil.Api.News.Dto.NewsResponseDTO;
import com.youtil.Api.News.Dto.NewsResponseDTO.NewsItem;
import com.youtil.Model.News;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NewsConverter {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static NewsResponseDTO.NewsItem toNewsItem(News news, String serverDomain) {
        String proxiedThumbnail = serverDomain + "/api/v1/news/image-proxy?url=" +
                URLEncoder.encode(news.getThumbnail(), StandardCharsets.UTF_8);

        return NewsItem.builder()
                .thumbnail(proxiedThumbnail)
                .title(news.getTitle())
                .summary(news.getContent())
                .link(news.getOriginUrl())
                .createdAt(news.getCreatedAt().toString())
                .build();
    }

    public static NewsResponseDTO.GetNewsResponse toGetNewsResponse(List<NewsItem> newsItems) {
        return NewsResponseDTO.GetNewsResponse.builder()
                .news(newsItems)
                .build();
    }

}

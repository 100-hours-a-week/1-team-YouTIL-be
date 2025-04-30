package com.youtil.Api.News.Converter;

import com.youtil.Api.News.Dto.NewsResponseDTO;
import com.youtil.Api.News.Dto.NewsResponseDTO.NewsItem;
import com.youtil.Model.News;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NewsConverter {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static NewsItem toNewsItem(News news) {
        return NewsItem.builder()
                .title(news.getTitle())
                .link(news.getOriginUrl())
                .summary(news.getContent())
                .thumbnail(news.getThumbnail())
                .createdAt(news.getCreatedAt().format(formatter)).build();
    }

    public static NewsResponseDTO.GetNewsResponse toGetNewsResponse(List<NewsItem> newsItems) {
        return NewsResponseDTO.GetNewsResponse.builder()
                .news(newsItems)
                .build();
    }

}

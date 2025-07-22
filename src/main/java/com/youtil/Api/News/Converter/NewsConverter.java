package com.youtil.Api.News.Converter;

import com.youtil.Api.News.Dto.NewsResponseDTO;
import com.youtil.Api.News.Dto.NewsResponseDTO.NewsItem;
import com.youtil.Model.News;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NewsConverter {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static NewsResponseDTO.NewsItem toNewsItem(News news, String serverDomain) {
        if (news.getThumbnail() == null) {
            return NewsItem.builder()
                    .thumbnail(
                            "https://youtil-bucket-dev.s3.ap-northeast-2.amazonaws.com/news/image.png")
                    .title(news.getTitle())
                    .summary(news.getContent())
                    .link(news.getOriginUrl())
                    .createdAt(news.getCreatedAt().toString())
                    .build();
        }

        return NewsItem.builder()
                .thumbnail(news.getThumbnail())
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

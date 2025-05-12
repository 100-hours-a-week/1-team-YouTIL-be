package com.youtil.Api.News.Dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

public class NewsResponseDTO {

    @Getter
    @Builder
    @Schema(description = "뉴스의 응답값")
    public static class GetNewsResponse {

        List<NewsItem> news;
    }

    @Getter
    @Builder
    public static class NewsItem {

        @Schema(description = "뉴스의 썸네일 입니다.")
        String thumbnail;
        @Schema(description = "뉴스의 제목입니다.")
        String title;
        @Schema(description = "뉴스의 요약 내용 입니다.")
        String summary;
        @Schema(description = "뉴스의 오리지널 URL 입니다.")
        String link;
        @Schema(description = "뉴스의 생성 시각 입니다.")
        String createdAt;

    }

}

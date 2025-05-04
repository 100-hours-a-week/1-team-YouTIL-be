package com.youtil.Api.News.Controller;

import com.youtil.Api.News.Dto.NewsResponseDTO.GetNewsResponse;
import com.youtil.Api.News.Service.NewsService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.MessageCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/news")
@Tag(name = "news", description = "뉴스와 관련된 API 입니다")
public class NewsController {

    private final NewsService newsService;

    @GetMapping("")
    ApiResponse<GetNewsResponse> GetNewsController() {
        return new ApiResponse<>(MessageCode.FIND_NEWS_SUCCESS.getMessage(), "200", newsService.getNewsService());
    }

    @PostMapping("")
    ApiResponse<String> PostNewsController() {
        newsService.createNewsService();
        return new ApiResponse<>("뉴스 생성에 성공했습니다!", "201");
    }
}

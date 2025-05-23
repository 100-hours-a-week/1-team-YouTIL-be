package com.youtil.Api.News.Controller;

import com.youtil.Api.News.Dto.NewsResponseDTO.GetNewsResponse;
import com.youtil.Api.News.Service.NewsService;
import com.youtil.Common.ApiResponse;
import com.youtil.Common.Enums.MessageCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/news")
@Tag(name = "news", description = "뉴스와 관련된 API 입니다")
public class NewsController {

    private final NewsService newsService;
    private final WebClient webClient;

    @GetMapping("")
    ResponseEntity<ApiResponse<GetNewsResponse>> GetNewsController() {
        return ResponseEntity.ok(
                new ApiResponse<>(MessageCode.FIND_NEWS_SUCCESS.getMessage(), "200",
                        newsService.getNewsService()));
    }

    @PostMapping("")
    ResponseEntity<ApiResponse<String>> PostNewsController() {
        newsService.createNewsService();
        return new ResponseEntity<>(new ApiResponse<>("뉴스 생성에 성공했습니다!", "201"), HttpStatus.CREATED);
    }

    @GetMapping("/image-proxy")
    public Mono<ResponseEntity<byte[]>> proxyImage(@RequestParam String url) {
        return webClient.get()
                .uri(url)
                .header("User-Agent", "Mozilla/5.0") // 핫링크 차단 우회
                .retrieve()
                .toEntity(byte[].class)
                .map(response -> {
                    MediaType contentType = response.getHeaders().getContentType();
                    return ResponseEntity
                            .ok()
                            .contentType(contentType != null ? contentType : MediaType.IMAGE_JPEG)
                            .body(response.getBody());
                });
    }
}

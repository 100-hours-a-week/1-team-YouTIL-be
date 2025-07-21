package com.youtil.Config;

import com.youtil.Common.DuplicatePrevention.DuplicatePreventionInterceptor;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig implements WebMvcConfigurer {

    private final DuplicatePreventionInterceptor duplicatePreventionInterceptor;
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120000) // 연결 타임아웃 10초
                .responseTimeout(Duration.ofSeconds(120000)) // 응답 타임아웃 10초
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(120000))   // 읽기 타임아웃
                        .addHandlerLast(new WriteTimeoutHandler(120000))); // 쓰기 타임아웃

        return WebClient.builder()
                .baseUrl("https://api.github.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(5 * 1024 * 1024)) // 5MB 제한
                .build();
    }

    // 인터셉터 설정 추가
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(duplicatePreventionInterceptor)
                .addPathPatterns("/api/**")  // 모든 API에 적용
                .excludePathPatterns(
                        "/api/v1/auth/**",   // 로그인/회원가입 제외
                        "/api/v1/public/**", // 공개 API 제외
                        "/api/v1/health"     // 헬스체크 제외
                );
    }
}

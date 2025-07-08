package com.youtil.Config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

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
}

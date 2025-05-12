package com.youtil.Config;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final Environment env;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        String[] activeProfiles = env.getActiveProfiles();
        boolean isDev = Arrays.asList(activeProfiles).contains("dev");

        if (isDev) {
            config.addAllowedOrigin("http://localhost:3000");
            config.addAllowedOrigin("http://34.22.84.164:3000");
            config.addAllowedOrigin("https://dev.youtil.co.kr");
        } else {
            config.addAllowedOrigin("https://youtil.co.kr");
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}

package com.youtil.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties(prefix = "github.oauth")
@Component
public class GithubOAuthProperties {

    private GithubApp local;
    private GithubApp dev;
    private GithubApp prod;

    @Getter
    @Setter
    public static class GithubApp {

        private String clientId;
        private String clientSecret;
    }
}


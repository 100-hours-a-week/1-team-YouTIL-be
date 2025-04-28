package com.youtil.Api.User.Service;

import com.youtil.Api.User.Dto.GitHubRequestDTO;
import com.youtil.Api.User.Dto.GithubResponseDTO.GitHubAccessTokenResponse;
import com.youtil.Api.User.Dto.UserResponseDTO;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final WebClient webClient;

    @Value("${github.client-id}")
    private String clientId;
    @Value("${github.client-secret}")
    private String clientSecret;
    public UserResponseDTO.LoginResponseDTO loginUserService(String authorizationCode){
        log.info(authorizationCode);
        String accessToken = getAccessToken(authorizationCode);

        return UserResponseDTO.LoginResponseDTO.builder().accessToken(accessToken).build();
    }
    private String getAccessToken(String authorizationCode) {
        String response = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .bodyValue(GitHubRequestDTO.GitHubAccessTokenRequest.builder().client_id(clientId)
                        .client_secret(clientSecret).code(authorizationCode)
                        .build())
                .retrieve().bodyToMono(String.class).block();
        log.info(response);
        if (response == null) {
            throw new RuntimeException("Get access token failed");
        }
        return response;
    }

}

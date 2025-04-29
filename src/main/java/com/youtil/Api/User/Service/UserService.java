package com.youtil.Api.User.Service;

import com.youtil.Api.User.Converter.UserConverter;
import com.youtil.Api.User.Dto.GitHubRequestDTO;
import com.youtil.Api.User.Dto.GithubResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Model.User;
import com.youtil.Repository.UserRepository;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.JwtUtil;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final WebClient webClient;
    private final UserRepository userRepository;
    private final TokenEncryptor tokenEncryptor;
    @Value("${github.client-id}")
    private String clientId;
    @Value("${github.client-secret}")
    private String clientSecret;

    @Transactional
    public UserResponseDTO.LoginResponseDTO loginUserService(String authorizationCode) {
        String accessToken = getAccessToken(authorizationCode);

        String email = getEmailInfo(accessToken);
        Optional<User> userOptional = userRepository.findByEmail(email);
        String encryptAccessToken = tokenEncryptor.encrypt(accessToken);
        //만약 존재하면 깃허브 엑세스 토큰만 교체 후 로그인
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setGithubToken(encryptAccessToken);
            return UserConverter.toUserResponseDTO(JwtUtil.generateAccessToken(user.getId()),
                    JwtUtil.generateRefreshToken(user.getId()));
        } else {
            GithubResponseDTO.GitHubUserInfo gitHubUserInfo = getUserInfo(accessToken);
            User user = UserConverter.toUser(email, gitHubUserInfo, encryptAccessToken);
            User newUser = userRepository.save(user);
            return UserConverter.toUserResponseDTO(JwtUtil.generateAccessToken(newUser.getId()),
                    JwtUtil.generateRefreshToken(newUser.getId()));
        }

    }

    //서비스 내장 함수

    private String getAccessToken(String authorizationCode) {

        GithubResponseDTO.GitHubAccessTokenResponse response = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .bodyValue(GitHubRequestDTO.GitHubAccessTokenRequest.builder().client_id(clientId)
                        .client_secret(clientSecret).code(authorizationCode)
                        .build())
                .retrieve().bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class).block();

        log.info(response.getAccess_token());
        log.info("스코프 : " + response.getScope());

        if (response == null || response.getAccess_token() == null) {
            throw new RuntimeException("Get access token failed");
        }
        return response.getAccess_token();
    }

    //프로필 가져오는 메서드
    private GithubResponseDTO.GitHubUserInfo getUserInfo(String accessToken) {

        GithubResponseDTO.GitHubUserInfo userInfo = webClient.get()
                .uri("https://api.github.com/user")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve().bodyToMono(GithubResponseDTO.GitHubUserInfo.class).block();

        if (userInfo == null) {
            throw new RuntimeException("Get user info failed");
        }
        return userInfo;
    }

    //프라이빗 이메일 가져오는 메서드
    private String getEmailInfo(String accessToken) {
        GithubResponseDTO.GitHubEmailInfo[] emails = webClient.get()
                .uri("https://api.github.com/user/emails")
                .headers(header -> header.setBearerAuth(accessToken))
                .retrieve().bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class).block();

        if (emails == null) {
            throw new RuntimeException("Get email info failed");
        }

        return Arrays.stream(emails)
                .filter(email -> email.isPrimary() && email.isVerified())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Get email info failed"))
                .getEmail();
    }
}

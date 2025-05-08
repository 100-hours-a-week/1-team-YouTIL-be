package com.youtil.Api.User.Service;

import com.youtil.Api.User.Converter.UserConverter;
import com.youtil.Api.User.Dto.GitHubRequestDTO;
import com.youtil.Api.User.Dto.GithubResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO.TilCountYearsItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Config.GithubOAuthProperties;
import com.youtil.Exception.UserException.UserException;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import com.youtil.Util.JwtUtil;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
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
    private final EntityValidator entityValidator;
    private final TilRepository tilRepository;
    private final GithubOAuthProperties github;


    @Transactional
    public UserResponseDTO.LoginResponseDTO loginUserService(String authorizationCode,
            String origin) {
        String accessToken = getAccessToken(authorizationCode, origin);

        String email = getEmailInfo(accessToken);
        Optional<User> userOptional = userRepository.findByEmail(email);
        String encryptAccessToken = tokenEncryptor.encrypt(accessToken);
        //만약 존재하면 깃허브 엑세스 토큰만 교체 후 로그인
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setGithubToken(encryptAccessToken);
            return UserConverter.toUserResponseDTO(JwtUtil.generateAccessToken(user.getId()),
                    JwtUtil.generateRefreshToken(user.getId()));
            //만약 존재하지 않다면 유저 계정 생성 후 로그인
        } else {
            GithubResponseDTO.GitHubUserInfo gitHubUserInfo = getUserInfo(accessToken);
            User user = UserConverter.toUser(email, gitHubUserInfo, encryptAccessToken);
            User newUser = userRepository.save(user);
            return UserConverter.toUserResponseDTO(JwtUtil.generateAccessToken(newUser.getId()),
                    JwtUtil.generateRefreshToken(newUser.getId()));
        }

    }

    public UserResponseDTO.GetUserInfoResponseDTO getUserInfoService(long userId) {
        User user = entityValidator.getValidUserOrThrow(userId);

        return UserConverter.toUserInfoResponseDTO(user);

    }

    @Transactional
    public void inactiveUserService(long userId) {
        User user = entityValidator.getValidUserOrThrow(userId);
        user.setStatus(Status.deactive);
    }

    public UserResponseDTO.GetUserTilCountResponseDTO getUserTilCountService(long userId,
            int year) {
        //유저 유효성 확인
        entityValidator.getValidUserOrThrow(userId);

        List<Til> tils = tilRepository.findAllByUserIdAndYear(userId, year);
        Map<Integer, List<Integer>> monthMap = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            int days = YearMonth.of(year, month).lengthOfMonth();
            monthMap.put(month, new ArrayList<>(Collections.nCopies(days, 0)));
        }

        for (Til til : tils) {
            LocalDate date = til.getCreatedAt().toLocalDate();
            int month = date.getMonthValue();
            int day = date.getDayOfMonth();
            List<Integer> days = monthMap.get(month);
            days.set(day - 1, days.get(day - 1) + 1);
        }

        TilCountYearsItem tilCountYearsItem = UserConverter.toUserTilCountYearsItem(monthMap);

        return UserConverter.toUserTilCountResponseDTO(year, tilCountYearsItem);
    }

    public UserResponseDTO.GetUserTilsResponseDTO getUserTilsService(long userId,
            Pageable pageable) {
        User user = entityValidator.getValidUserOrThrow(userId);
        List<UserResponseDTO.TilListItem> tilList = tilRepository.findUserTils(user.getId(),
                pageable);
        return UserConverter.toUserTilsResponseDTO(tilList);
    }

    //서비스 내장 함수

    private String getAccessToken(String authorizationCode, String origin) {
        GithubOAuthProperties.GithubApp app = resolveAppByOrigin(origin);
        GithubResponseDTO.GitHubAccessTokenResponse response = webClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .bodyValue(GitHubRequestDTO.GitHubAccessTokenRequest.builder()
                        .client_id(app.getClientId())
                        .client_secret(app.getClientSecret())
                        .code(authorizationCode)
                        .build())
                .retrieve()
                .bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class)
                .block();

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
            throw new UserException.GitHubEmailNotFoundException();
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
            throw new UserException.GitHubEmailNotFoundException();
        }

        return Arrays.stream(emails)
                .filter(email -> email.isPrimary() && email.isVerified())
                .findFirst()
                .orElseThrow(UserException.GitHubEmailNotFoundException::new)
                .getEmail();
    }

    //분기 로직
    private GithubOAuthProperties.GithubApp resolveAppByOrigin(String origin) {
        if (origin.contains("localhost")) {
            return github.getLocal();
        }
        if (origin.contains("34.22.84.164") || origin.contains("dev.")) {
            return github.getDev();
        }
        return github.getProd();
    }

}

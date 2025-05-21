package com.youtil.Api.User.Service;

import com.youtil.Api.User.Dto.GithubResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO.GetUserTilCountResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO.TilListItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Config.GithubOAuthProperties;
import com.youtil.Exception.UserException.UserException.GitHubEmailNotFoundException;
import com.youtil.Exception.UserException.UserException.GitHubProfileNotFoundException;
import com.youtil.Exception.UserException.UserException.UserNotFoundException;
import com.youtil.Exception.UserException.UserException.WrongAuthorizationCodeException;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import com.youtil.Util.JwtUtil;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import org.assertj.core.util.Lists;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private static final Logger log = LoggerFactory.getLogger(UserServiceTest.class);
    @Mock
    private UserRepository userRepository;
    @Mock
    private TilRepository tilRepository;
    @Mock
    private EntityValidator entityValidator;
    @InjectMocks
    private UserService userService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;
    @Mock
    private TokenEncryptor tokenEncryptor;
    @Mock
    private GithubOAuthProperties github;
    @Mock
    private GithubOAuthProperties.GithubApp githubApp;


    private User createMockUser() {
        User user = User.builder()
                .id(1L)
                .email("test@email.com")
                .status(Status.active)
                .githubToken("accessToken")
                .nickname("nick")
                .profileImageUrl("profile-image")
                .build();
        return user;
    }

    private Til createMockTil() {
        Til til = Til.builder()
                .id(1L)
                .user(createMockUser())
                .status(Status.active)
                .title("title")
                .content("content")
                .tag(Lists.newArrayList("tag1", "tag2"))
                .category("FULLSTACK")
                .commentsCount(0)
                .visitedCount(0)
                .isDisplay(true)
                .recommendCount(0).build();
        return til;
    }

    //테스트코드는 행위 기반으로 서술하기 때문에 스네이크 패턴을 혼용해서 사용한다,
    //Ex) methodName_condition_expectedResult()

    //유저 로그인 관련
    @SuppressWarnings("unchecked") // 경고 무시
    @Test
    @DisplayName("유저 로그인 - 이미 계정이 있을경우 - 로그인 성공")
    void loginUser_withValidCredentialsAndValidUser_success() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";
        String email = "test@email.com";
        String encryptedToken = "encryptedToken";
        User mockUser = createMockUser();

        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(
                WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(
                (WebClient.RequestHeadersSpec) headersSpec); // ❗ raw 타입으로 우회
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class))
                .thenReturn(Mono.just(tokenResponse));

        GithubResponseDTO.GitHubEmailInfo[] emails = {
                GithubResponseDTO.GitHubEmailInfo.builder()
                        .email(email)
                        .primary(true)
                        .verified(true)
                        .build()
        };

        WebClient.RequestHeadersUriSpec emailUriSpec = mock(
                WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec emailHeadersSpec = mock(
                WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec emailResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(emailUriSpec);
        when(emailUriSpec.uri(eq("https://api.github.com/user/emails"))).thenReturn(
                emailHeadersSpec);
        when(emailHeadersSpec.headers(any())).thenReturn(emailHeadersSpec);
        when(emailHeadersSpec.retrieve()).thenReturn(emailResponseSpec);
        when(emailResponseSpec.bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class)).thenReturn(
                Mono.just(emails));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(tokenEncryptor.encrypt(accessToken)).thenReturn(encryptedToken);
        when(github.getLocal()).thenReturn(githubApp);
        when(githubApp.getClientId()).thenReturn("mockClientId");
        when(githubApp.getClientSecret()).thenReturn("mockClientSecret");

        try (MockedStatic<JwtUtil> jwtUtilMockedStatic = mockStatic(JwtUtil.class)) {
            jwtUtilMockedStatic.when(() -> JwtUtil.generateAccessToken(mockUser.getId()))
                    .thenReturn("JWTAccessToken");
            jwtUtilMockedStatic.when(() -> JwtUtil.generateRefreshToken(mockUser.getId()))
                    .thenReturn("JWTRefreshToken");

            UserResponseDTO.LoginResponseDTO result = userService.loginUserService(
                    authorizationCode, origin);

            assertEquals("JWTAccessToken", result.getAccessToken());
            assertEquals("JWTRefreshToken", result.getRefreshToken());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("유저 로그인 - 유저 정보 없음 - 로그인 성공")
    void loginUser_withInvalidCredentialsAndValidUser_success() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";
        String email = "test@email.com";
        String encryptedToken = "encryptedToken";
        User newUser = createMockUser();
        newUser.setId(2L);

        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class)).thenReturn(
                Mono.just(tokenResponse));

        GithubResponseDTO.GitHubEmailInfo[] emails = {
                GithubResponseDTO.GitHubEmailInfo.builder()
                        .email(email)
                        .primary(true)
                        .verified(true)
                        .build()
        };

        GithubResponseDTO.GitHubUserInfo userInfo = GithubResponseDTO.GitHubUserInfo.builder()
                .login("jun")
                .avatar_url("https://avatars.githubusercontent.com/u/123456")
                .build();

        WebClient.RequestHeadersUriSpec emailUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec emailHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec emailResponseSpec = mock(WebClient.ResponseSpec.class);

        WebClient.RequestHeadersUriSpec userUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec userHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec userResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(emailUriSpec).thenReturn(userUriSpec);
        when(emailUriSpec.uri(eq("https://api.github.com/user/emails"))).thenReturn(
                emailHeaderSpec);
        when(emailHeaderSpec.headers(any())).thenReturn(emailHeaderSpec);
        when(emailHeaderSpec.retrieve()).thenReturn(emailResponseSpec);
        when(emailResponseSpec.bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class)).thenReturn(
                Mono.just(emails));

        when(userUriSpec.uri(eq("https://api.github.com/user"))).thenReturn(userHeaderSpec);
        when(userHeaderSpec.headers(any())).thenReturn(userHeaderSpec);
        when(userHeaderSpec.retrieve()).thenReturn(userResponseSpec);
        when(userResponseSpec.bodyToMono(GithubResponseDTO.GitHubUserInfo.class)).thenReturn(
                Mono.just(userInfo));

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(tokenEncryptor.encrypt(accessToken)).thenReturn(encryptedToken);
        when(github.getLocal()).thenReturn(githubApp);
        when(githubApp.getClientId()).thenReturn("mockClientId");
        when(githubApp.getClientSecret()).thenReturn("mockClientSecret");

        try (MockedStatic<JwtUtil> jwt = mockStatic(JwtUtil.class)) {
            jwt.when(() -> JwtUtil.generateAccessToken(newUser.getId()))
                    .thenReturn("JWTAccessToken");
            jwt.when(() -> JwtUtil.generateRefreshToken(newUser.getId()))
                    .thenReturn("JWTRefreshToken");

            UserResponseDTO.LoginResponseDTO result = userService.loginUserService(
                    authorizationCode, origin);

            assertEquals("JWTAccessToken", result.getAccessToken());
            assertEquals("JWTRefreshToken", result.getRefreshToken());
            verify(userRepository).save(any(User.class));
        }
    }

    @Test
    @DisplayName("유저 로그인 - 인가코드 잘못됨- 로그인 실패")
    void loginUser_withWrongAuthorizationCode_fail() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";

        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class)).thenThrow(
                WrongAuthorizationCodeException.class);
        when(github.getLocal()).thenReturn(githubApp);
        when(githubApp.getClientId()).thenReturn("mockClientId");
        when(githubApp.getClientSecret()).thenReturn("mockClientSecret");

        assertThatThrownBy(() -> userService.loginUserService(authorizationCode, origin))
                .isInstanceOf(WrongAuthorizationCodeException.class);
    }

    @Test
    @DisplayName("유저 로그인 - 아메일 스코프 권한 없음 - 로그인 실패")
    void loginUser_withWrongScope_fail() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";

        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class)).thenReturn(
                Mono.just(tokenResponse));

        //깃허브 APP 모킹
        when(github.getLocal()).thenReturn(githubApp);
        when(githubApp.getClientId()).thenReturn("mockClientId");
        when(githubApp.getClientSecret()).thenReturn("mockClientSecret");

        WebClient.RequestHeadersUriSpec emailUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec emailHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec emailResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(emailUriSpec);
        when(emailUriSpec.uri(eq("https://api.github.com/user/emails"))).thenReturn(
                emailHeaderSpec);
        when(emailHeaderSpec.headers(any())).thenReturn(emailHeaderSpec);
        when(emailHeaderSpec.retrieve()).thenReturn(emailResponseSpec);
        when(emailResponseSpec.bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class)).thenThrow(
                GitHubEmailNotFoundException.class);

        assertThatThrownBy(
                () -> userService.loginUserService(authorizationCode, origin)).isInstanceOf(
                GitHubEmailNotFoundException.class);
    }

    @Test
    @DisplayName("유저 로그인 - 유저 정보 스코프 권한 없음 - 로그인 실패")
    void loginUser_withWrongScopeUserInfo_fail() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";
        String email = "test@email.com";

        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class)).thenReturn(
                Mono.just(tokenResponse));
        //깃허브 APP 모킹
        when(github.getLocal()).thenReturn(githubApp);
        when(githubApp.getClientId()).thenReturn("mockClientId");
        when(githubApp.getClientSecret()).thenReturn("mockClientSecret");

        GithubResponseDTO.GitHubEmailInfo[] emails = {
                GithubResponseDTO.GitHubEmailInfo.builder()
                        .email(email)
                        .primary(true)
                        .verified(true)
                        .build()
        };

        WebClient.RequestHeadersUriSpec emailUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec emailHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec emailResponseSpec = mock(WebClient.ResponseSpec.class);

        WebClient.RequestHeadersUriSpec userUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec userHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec userResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(emailUriSpec).thenReturn(userUriSpec);
        when(emailUriSpec.uri(eq("https://api.github.com/user/emails"))).thenReturn(
                emailHeaderSpec);
        when(emailHeaderSpec.headers(any())).thenReturn(emailHeaderSpec);
        when(emailHeaderSpec.retrieve()).thenReturn(emailResponseSpec);
        when(emailResponseSpec.bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class)).thenReturn(
                Mono.just(emails));

        when(userUriSpec.uri(eq("https://api.github.com/user"))).thenReturn(userHeaderSpec);
        when(userHeaderSpec.headers(any())).thenReturn(userHeaderSpec);
        when(userHeaderSpec.retrieve()).thenReturn(userResponseSpec);
        when(userResponseSpec.bodyToMono(GithubResponseDTO.GitHubUserInfo.class)).thenThrow(
                GitHubProfileNotFoundException.class);

        assertThatThrownBy(
                () -> userService.loginUserService(authorizationCode, origin)).isInstanceOf(
                GitHubProfileNotFoundException.class);
    }

    //유저 정보 조회
    @Test
    @DisplayName("유저 정보 조회 -값이 있는 경우 - 조회 성공")
    void findUser_withValidUser_success() {

        User mockUser = createMockUser();
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);

        UserResponseDTO.GetUserInfoResponseDTO userInfo = userService.getUserInfoService(
                mockUser.getId());
        assertEquals(mockUser.getNickname(), userInfo.getName());

    }

    @Test
    @DisplayName("유저 정보 조회 - 유저가 탈퇴했거나 존재 하지 않는 경우 - 조회 실패")
    void findUser_withInvalidUser_fail() {

        User mockUser = createMockUser();
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);
        assertThatThrownBy(() -> userService.getUserInfoService(mockUser.getId())).isInstanceOf(
                UserNotFoundException.class);
    }

    //유저 탈퇴
    @Test
    @DisplayName("유저 탈퇴 - 유저 존재 - 탈퇴 성공")
    void inActiveUser_withValidUser_success() {
        User mockUser = createMockUser();
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);

        userService.inactiveUserService(mockUser.getId());
        assertEquals(Status.deactive, mockUser.getStatus());
    }

    @Test
    @DisplayName("유저 탈퇴 - 유저가 이미 탈퇴했거나 존재하지 않는 경우 - 탈퇴 실패")
    void inActiveUser_withInvalidUser_fail() {
        User mockUser = createMockUser();
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);
        assertThatThrownBy(() -> userService.inactiveUserService(mockUser.getId())).isInstanceOf(
                UserNotFoundException.class);
    }

    //유저 TIL 작성 글 조회
    @Test
    @DisplayName("유저 TIL 작성 글 조회 - 유저가 존재하고 값이 존재할 경우 - 조회 성공")
    void findUserPost_withValidUserAndValidPost_success() {
        User mockUser = createMockUser();
        Til mockTil = createMockTil();
        TilListItem tilListItem = TilListItem.builder()
                .tilId(mockTil.getId())
                .userName(mockUser.getNickname())
                .id(mockUser.getId())
                .title(mockTil.getTitle())
                .tags(mockTil.getTag())
                .userProfileImageUrl(mockUser.getProfileImageUrl())
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(tilRepository.findUserTils(mockUser.getId(), pageable)).thenReturn(
                Lists.newArrayList(tilListItem, tilListItem));
        UserResponseDTO.GetUserTilsResponseDTO getUserTilsResponseDTO = userService.getUserTilsService(
                mockUser.getId(), pageable);

        assertEquals(getUserTilsResponseDTO.getTils().get(0).getTitle(), tilListItem.getTitle());

    }

    @Test
    @DisplayName("유저 TIL 작성글 조회 -유저가 존재하고 값이 존재하지 않을 경우 - 조회 성공 ")
    void findUserPost_withInvalidUserAndValidPost_success() {
        User mockUser = createMockUser();

        Pageable pageable = PageRequest.of(0, 20);

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(tilRepository.findUserTils(mockUser.getId(), pageable)).thenReturn(null);

        UserResponseDTO.GetUserTilsResponseDTO getUserTilsResponseDTO = userService.getUserTilsService(
                mockUser.getId(), pageable);
        assertEquals(getUserTilsResponseDTO.getTils(), null);
    }

    @Test
    @DisplayName("유저 TIL 작성글 조회 - 유저가 존재하지 않을 경우 - 조회 실패")
    void findUserPost_withInvalidUser_fail() {
        User mockUser = createMockUser();
        Pageable pageable = PageRequest.of(0, 20);
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);
        assertThatThrownBy(
                () -> userService.getUserTilsService(mockUser.getId(), pageable)).isInstanceOf(
                UserNotFoundException.class);
    }


    //유저 TIL 기록 조회
    @Test
    @DisplayName("유저 TIL 기록 조회 - 유저가 존재할 경우 - 조회 성공")
    void findUserTilCount_withValidUser_success() {
        User mockUser = createMockUser();
        Til mockTil = createMockTil();
        mockTil.setCreatedAt(OffsetDateTime.of(
                2025, 5, 25, 13, 29, 19, 0,
                ZoneOffset.ofHours(9)
        ));

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(tilRepository.findAllByUserIdAndYear(mockUser.getId(), 2025)).thenReturn(
                Lists.newArrayList(mockTil, mockTil, mockTil, mockTil, mockTil));
        GetUserTilCountResponseDTO getUserTilCountResponseDTO = userService.getUserTilCountService(
                mockUser.getId(), 2025);

        assertEquals(getUserTilCountResponseDTO.getTils().getMay().get(24), 5);
    }

    @Test
    @DisplayName("유저 TIL 기록 조회 - 유저가 존재하지 않을 경우 - 조회 실패")
    void findUserTilCount_withInvalidUser_fail() {
        User mockUser = createMockUser();
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);
        assertThatThrownBy(
                () -> userService.getUserTilCountService(mockUser.getId(), 2025)).isInstanceOf(
                UserNotFoundException.class);
    }

}

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
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TilRepository tilRepository;
    @Mock
    private EntityValidator entityValidator;
    @Mock(lenient = true)
    private WebClient webClient;
    @Mock
    private TokenEncryptor tokenEncryptor;
    @Mock
    private GithubOAuthProperties github;
    @Mock
    private GithubOAuthProperties.GithubApp githubApp;
    @InjectMocks
    private UserService userService;

    private User mockUser;
    private Til mockTil;

    private WebClient.RequestBodyUriSpec uriSpec;
    private WebClient.RequestBodySpec bodySpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;

    private WebClient.RequestHeadersUriSpec getEmailUriSpec;
    private WebClient.RequestHeadersSpec getEmailHeaderSpec;
    private WebClient.ResponseSpec getEmailResponseSpec;
    private WebClient.RequestHeadersUriSpec getUserUriSpec;
    private WebClient.RequestHeadersSpec getUserHeaderSpec;
    private WebClient.ResponseSpec getUserResponseSpec;
    private MockedStatic<JwtUtil> jwtUtilStatic;

    @BeforeEach
    void setup() {
        mockUser = createMockUser();
        mockTil = createMockTil();

    }

    @AfterEach
    void tearDown() {
        if (jwtUtilStatic != null) {
            jwtUtilStatic.close();
            jwtUtilStatic = null;
        }
    }

    //테스트코드는 행위 기반으로 서술하기 때문에 스네이크 패턴을 혼용해서 사용한다,
    //Ex) methodName_condition_expectedResult()

    //유저 로그인 관련
    @Test
    @DisplayName("유저 로그인 - 이미 계정이 있을경우 - 로그인 성공")
    void loginUser_withValidCredentialsAndValidUser_success() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";
        String email = mockUser.getEmail();
        String encryptedToken = "encryptedToken";
        setupWebClient();
        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class))
                .thenReturn(Mono.just(tokenResponse));

        mockGithubAppProps();
        mockEmailAPI(email);
        mockJwt(mockUser.getId());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(tokenEncryptor.encrypt(accessToken)).thenReturn(encryptedToken);

        UserResponseDTO.LoginResponseDTO result = userService.loginUserService(
                authorizationCode, origin);

        assertEquals("JWTAccessToken", result.getAccessToken());
        assertEquals("JWTRefreshToken", result.getRefreshToken());
    }

    @Test
    @DisplayName("유저 로그인 - 유저 정보 없음 - 로그인 성공")
    void loginUser_withInvalidCredentialsAndValidUser_success() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";
        String email = mockUser.getEmail();
        String encryptedToken = "encryptedToken";

        User newUser = createMockUser();
        setupWebClient();
        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class))
                .thenReturn(Mono.just(tokenResponse));

        mockGithubAppProps();
        mockEmailAPI(email);
        mockUserInfoAPI();
        mockJwt(newUser.getId());

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(tokenEncryptor.encrypt(accessToken)).thenReturn(encryptedToken);

        UserResponseDTO.LoginResponseDTO result = userService.loginUserService(
                authorizationCode, origin);

        assertEquals("JWTAccessToken", result.getAccessToken());
        assertEquals("JWTRefreshToken", result.getRefreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("유저 로그인 - 인가코드 잘못됨- 로그인 실패")
    void loginUser_withWrongAuthorizationCode_fail() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        setupWebClient();
        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class))
                .thenThrow(WrongAuthorizationCodeException.class);

        mockGithubAppProps();

        assertThatThrownBy(() -> userService.loginUserService(authorizationCode, origin))
                .isInstanceOf(WrongAuthorizationCodeException.class);
    }

    @Test
    @DisplayName("유저 로그인 - 아메일 스코프 권한 없음 - 로그인 실패")
    void loginUser_withWrongScope_fail() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";
        setupWebClient();
        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class))
                .thenReturn(Mono.just(tokenResponse));

        mockGithubAppProps();

        when(getEmailUriSpec.uri(eq("https://api.github.com/user/emails"))).thenReturn(
                getEmailHeaderSpec);
        when(getEmailHeaderSpec.headers(any())).thenReturn(getEmailHeaderSpec);
        when(getEmailHeaderSpec.retrieve()).thenReturn(getEmailResponseSpec);
        when(getEmailResponseSpec.bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class))
                .thenThrow(GitHubEmailNotFoundException.class);

        assertThatThrownBy(() -> userService.loginUserService(authorizationCode, origin))
                .isInstanceOf(GitHubEmailNotFoundException.class);
    }

    @Test
    @DisplayName("유저 로그인 - 유저 정보 스코프 권한 없음 - 로그인 실패")
    void loginUser_withWrongScopeUserInfo_fail() {
        String authorizationCode = "authorization_code";
        String origin = "localhost";
        String accessToken = "accessToken";
        String email = mockUser.getEmail();
        setupWebClient();
        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(accessToken)
                        .build();

        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class))
                .thenReturn(Mono.just(tokenResponse));

        mockGithubAppProps();
        mockEmailAPI(email);

        when(getUserUriSpec.uri(eq("https://api.github.com/user"))).thenReturn(getUserHeaderSpec);
        when(getUserHeaderSpec.headers(any())).thenReturn(getUserHeaderSpec);
        when(getUserHeaderSpec.retrieve()).thenReturn(getUserResponseSpec);
        when(getUserResponseSpec.bodyToMono(GithubResponseDTO.GitHubUserInfo.class))
                .thenThrow(GitHubProfileNotFoundException.class);

        assertThatThrownBy(() -> userService.loginUserService(authorizationCode, origin))
                .isInstanceOf(GitHubProfileNotFoundException.class);
    }

    //유저 정보 조회
    @Test
    @DisplayName("유저 정보 조회 -값이 있는 경우 - 조회 성공")
    void findUser_withValidUser_success() {
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);

        UserResponseDTO.GetUserInfoResponseDTO userInfo = userService.getUserInfoService(
                mockUser.getId());

        assertEquals(mockUser.getNickname(), userInfo.getName());
    }

    @Test
    @DisplayName("유저 정보 조회 - 유저가 탈퇴했거나 존재 하지 않는 경우 - 조회 실패")
    void findUser_withInvalidUser_fail() {
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);

        assertThatThrownBy(() -> userService.getUserInfoService(mockUser.getId()))
                .isInstanceOf(UserNotFoundException.class);
    }

    //유저 탈퇴
    @Test
    @DisplayName("유저 탈퇴 - 유저 존재 - 탈퇴 성공")
    void inActiveUser_withValidUser_success() {
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);

        userService.inactiveUserService(mockUser.getId());

        assertEquals(Status.deactive, mockUser.getStatus());
    }

    @Test
    @DisplayName("유저 탈퇴 - 유저가 이미 탈퇴했거나 존재하지 않는 경우 - 탈퇴 실패")
    void inActiveUser_withInvalidUser_fail() {
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);

        assertThatThrownBy(() -> userService.inactiveUserService(mockUser.getId())).isInstanceOf(
                UserNotFoundException.class);
    }

    //유저 TIL 작성 글 조회
    @Test
    @DisplayName("유저 TIL 작성 글 조회 - 유저가 존재하고 값이 존재할 경우 - 조회 성공")
    void findUserPost_withValidUserAndValidPost_success() {
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

        UserResponseDTO.GetUserTilsResponseDTO getUserTilsResponseDTO =
                userService.getUserTilsService(mockUser.getId(), pageable);

        assertEquals(getUserTilsResponseDTO.getTils().get(0).getTitle(), tilListItem.getTitle());
    }

    @Test
    @DisplayName("유저 TIL 작성글 조회 - 유저가 존재하고 값이 존재하지 않을 경우 - 조회 성공")
    void findUserPost_withValidUserAndNoPost_success() {
        Pageable pageable = PageRequest.of(0, 20);
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(tilRepository.findUserTils(mockUser.getId(), pageable)).thenReturn(null);

        UserResponseDTO.GetUserTilsResponseDTO getUserTilsResponseDTO =
                userService.getUserTilsService(mockUser.getId(), pageable);

        assertEquals(getUserTilsResponseDTO.getTils(), null);
    }

    @Test
    @DisplayName("유저 TIL 작성글 조회 - 유저가 존재하지 않을 경우 - 조회 실패")
    void findUserPost_withInvalidUser_fail() {
        Pageable pageable = PageRequest.of(0, 20);
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);

        assertThatThrownBy(() -> userService.getUserTilsService(mockUser.getId(), pageable))
                .isInstanceOf(UserNotFoundException.class);
    }


    //유저 TIL 기록 조회
    @Test
    @DisplayName("유저 TIL 기록 조회 - 유저가 존재할 경우 - 조회 성공")
    void findUserTilCount_withValidUser_success() {

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
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);

        assertThatThrownBy(
                () -> userService.getUserTilCountService(mockUser.getId(), 2025)).isInstanceOf(
                UserNotFoundException.class);
    }


    //모듈화 코드
    private User createMockUser() {
        return User.builder()
                .id(1L)
                .email("test@email.com")
                .status(Status.active)
                .githubToken("accessToken")
                .nickname("nick")
                .profileImageUrl("profile-image")
                .build();
    }

    private Til createMockTil() {
        return Til.builder()
                .id(1L)
                .user(mockUser)
                .status(Status.active)
                .title("title")
                .content("content")
                .tag(Lists.newArrayList("tag1", "tag2"))
                .category("FULLSTACK")
                .commentsCount(0)
                .visitedCount(0)
                .isDisplay(true)
                .recommendCount(0)
                .build();
    }

    private void setupWebClient() {
        // POST
        uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        // GET - Email 정보와 유저 정보
        getEmailUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        getUserUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        getEmailHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        getUserHeaderSpec = mock(WebClient.RequestHeadersSpec.class);

        getEmailResponseSpec = mock(WebClient.ResponseSpec.class);
        getUserResponseSpec = mock(WebClient.ResponseSpec.class);
        //처음 get을 호출하면 이메일, 두번째 호출하면 user 정보로 규정한다.
        when(webClient.get())
                .thenReturn(getEmailUriSpec)
                .thenReturn(getUserUriSpec);
    }

    private void mockGithubAppProps() {
        //서비스 내부의 깃허브App을 설정한다.
        when(github.getLocal()).thenReturn(githubApp);
        when(githubApp.getClientId()).thenReturn("mockClientId");
        when(githubApp.getClientSecret()).thenReturn("mockClientSecret");
    }

    private void mockEmailAPI(String email) {
        GithubResponseDTO.GitHubEmailInfo[] emails = {
                GithubResponseDTO.GitHubEmailInfo.builder()
                        .email(email)
                        .primary(true)
                        .verified(true)
                        .build()
        };

        when(getEmailUriSpec.uri(eq("https://api.github.com/user/emails"))).thenReturn(
                getEmailHeaderSpec);
        when(getEmailHeaderSpec.headers(any())).thenReturn(getEmailHeaderSpec);
        when(getEmailHeaderSpec.retrieve()).thenReturn(getEmailResponseSpec);
        when(getEmailResponseSpec.bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class))
                .thenReturn(Mono.just(emails));
    }

    private void mockUserInfoAPI() {
        GithubResponseDTO.GitHubUserInfo userInfo = GithubResponseDTO.GitHubUserInfo.builder()
                .login("jun")
                .avatar_url("https://avatars.githubusercontent.com/u/123456")
                .build();

        when(getUserUriSpec.uri(eq("https://api.github.com/user"))).thenReturn(getUserHeaderSpec);
        when(getUserHeaderSpec.headers(any())).thenReturn(getUserHeaderSpec);
        when(getUserHeaderSpec.retrieve()).thenReturn(getUserResponseSpec);
        when(getUserResponseSpec.bodyToMono(GithubResponseDTO.GitHubUserInfo.class))
                .thenReturn(Mono.just(userInfo));
    }

    private void mockJwt(long userId) {
        jwtUtilStatic = Mockito.mockStatic(JwtUtil.class);
        jwtUtilStatic.when(() -> JwtUtil.generateAccessToken(userId))
                .thenReturn("JWTAccessToken");
        jwtUtilStatic.when(() -> JwtUtil.generateRefreshToken(userId))
                .thenReturn("JWTRefreshToken");
    }

}

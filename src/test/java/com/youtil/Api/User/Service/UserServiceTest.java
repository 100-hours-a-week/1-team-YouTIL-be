package com.youtil.Api.User.Service;

import com.youtil.Api.User.Dto.GithubResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO.GetUserTilCountResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO.TilListItem;
import com.youtil.Common.Enums.Status;
import com.youtil.Config.GithubOAuthProperties;
import static com.youtil.Constants.UserServiceConstants.ACCESS_TOKEN;
import static com.youtil.Constants.UserServiceConstants.AUTHORIZATION_CODE;
import static com.youtil.Constants.UserServiceConstants.EMAIL_URI;
import static com.youtil.Constants.UserServiceConstants.ENCRYPTED_TOKEN;
import static com.youtil.Constants.UserServiceConstants.JWT_ACCESS_TOKEN;
import static com.youtil.Constants.UserServiceConstants.JWT_REFRESH_TOKEN;
import static com.youtil.Constants.UserServiceConstants.MOCK_CLIENT_ID;
import static com.youtil.Constants.UserServiceConstants.MOCK_CLIENT_SECRET;
import static com.youtil.Constants.UserServiceConstants.ORIGIN;
import static com.youtil.Constants.UserServiceConstants.PAGEABLE;
import static com.youtil.Constants.UserServiceConstants.USER_SPEC_URI;
import com.youtil.Exception.UserException.UserException.GitHubEmailNotFoundException;
import com.youtil.Exception.UserException.UserException.GitHubProfileNotFoundException;
import com.youtil.Exception.UserException.UserException.UserNotFoundException;
import com.youtil.Exception.UserException.UserException.WrongAuthorizationCodeException;
import static com.youtil.Mock.MockTilBuilder.createMockTil;
import static com.youtil.Mock.MockUserBuilder.createMockUser;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import com.youtil.Util.JwtUtil;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import org.assertj.core.util.Lists;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
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
    @Mock
    private JwtUtil jwtUtil;
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


    @BeforeEach
    void setup() {
        mockUser = createMockUser();
        mockTil = createMockTil(mockUser);

    }


    //유저 로그인 관련
    @Test
    @DisplayName("유저 로그인 - 이미 계정이 있을경우 - 로그인 성공")
    void loginUser_withValidCredentialsAndValidUser_success() {
        final String email = mockUser.getEmail();
        setupWebClient();

        mockGithubAppProps();
        mockGithubToken();
        mockEmailAPI(email);
        mockJwt(mockUser.getId());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));
        when(tokenEncryptor.encrypt(ACCESS_TOKEN)).thenReturn(ENCRYPTED_TOKEN);

        UserResponseDTO.LoginResponseDTO result = userService.loginUserService(
                AUTHORIZATION_CODE, ORIGIN);

        verify(tokenEncryptor).encrypt(ACCESS_TOKEN);
        assertEquals(JWT_ACCESS_TOKEN, result.getAccessToken());
        assertEquals(JWT_REFRESH_TOKEN, result.getRefreshToken());
    }

    @Test
    @DisplayName("유저 로그인 - 유저 정보 없음(회원가입 후 로그인) - 로그인 성공")
    void loginUser_withInvalidCredentialsAndValidUser_success() {

        String email = mockUser.getEmail();
        User newUser = createMockUser();

        setupWebClient();

        mockGithubAppProps();
        mockGithubToken();
        mockEmailAPI(email);
        mockUserInfoAPI();
        mockJwt(newUser.getId());

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(tokenEncryptor.encrypt(ACCESS_TOKEN)).thenReturn(ENCRYPTED_TOKEN);

        UserResponseDTO.LoginResponseDTO result = userService.loginUserService(
                AUTHORIZATION_CODE, ORIGIN);

        assertEquals(JWT_ACCESS_TOKEN, result.getAccessToken());
        assertEquals(JWT_REFRESH_TOKEN, result.getRefreshToken());
        verify(tokenEncryptor).encrypt(ACCESS_TOKEN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("유저 로그인 - 인가코드 잘못됨- 로그인 실패")
    void loginUser_withWrongAuthorizationCode_fail() {

        setupWebClient();
        //인가코드 에러
        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class))
                .thenThrow(WrongAuthorizationCodeException.class);

        mockGithubAppProps();

        assertThatThrownBy(() -> userService.loginUserService(AUTHORIZATION_CODE, ORIGIN))
                .isInstanceOf(WrongAuthorizationCodeException.class);
    }

    @Test
    @DisplayName("유저 로그인 - 아메일 스코프 권한 없음 - 로그인 실패")
    void loginUser_withWrongScope_fail() {

        setupWebClient();
        mockGithubAppProps();

        mockGithubToken();

        when(getEmailUriSpec.uri(eq(EMAIL_URI))).thenReturn(
                getEmailHeaderSpec);
        when(getEmailHeaderSpec.headers(any())).thenReturn(getEmailHeaderSpec);
        when(getEmailHeaderSpec.retrieve()).thenReturn(getEmailResponseSpec);
        when(getEmailResponseSpec.bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class))
                .thenThrow(GitHubEmailNotFoundException.class);

        assertThatThrownBy(() -> userService.loginUserService(AUTHORIZATION_CODE, ORIGIN))
                .isInstanceOf(GitHubEmailNotFoundException.class);
    }


    @Test
    @DisplayName("유저 로그인 - 유저 정보 스코프 권한 없음 - 로그인 실패")
    void loginUser_withWrongScopeUserInfo_fail() {

        final String email = mockUser.getEmail();
        setupWebClient();

        mockGithubAppProps();
        mockGithubToken();
        mockEmailAPI(email);

        when(getUserUriSpec.uri(eq(USER_SPEC_URI))).thenReturn(getUserHeaderSpec);
        when(getUserHeaderSpec.headers(any())).thenReturn(getUserHeaderSpec);
        when(getUserHeaderSpec.retrieve()).thenReturn(getUserResponseSpec);
        when(getUserResponseSpec.bodyToMono(GithubResponseDTO.GitHubUserInfo.class))
                .thenThrow(GitHubProfileNotFoundException.class);

        assertThatThrownBy(() -> userService.loginUserService(AUTHORIZATION_CODE, ORIGIN))
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
        assertEquals(mockUser.getId(), userInfo.getUserId());
        assertEquals(mockUser.getProfileImageUrl(), userInfo.getProfileUrl());
        assertEquals(mockUser.getDescription(), userInfo.getDescription());
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
        List<TilListItem> tillLists = Lists.newArrayList(tilListItem, tilListItem);

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(tilRepository.findUserTils(mockUser.getId(), PAGEABLE)).thenReturn(
                tillLists);

        UserResponseDTO.GetUserTilsResponseDTO getUserTilsResponseDTO =
                userService.getUserTilsService(mockUser.getId(), PAGEABLE);

        assertEquals(getUserTilsResponseDTO.getTils().size(), tillLists.size());
        for (int i = 0; i < getUserTilsResponseDTO.getTils().size(); i++) {
            TilListItem actualTilListItem = getUserTilsResponseDTO.getTils().get(i);
            //내부 요소 검증
            assertEquals(tilListItem.getTilId(), actualTilListItem.getTilId());
            assertEquals(tilListItem.getUserName(), actualTilListItem.getUserName());
            assertEquals(tilListItem.getId(), actualTilListItem.getId());
            assertEquals(tilListItem.getTitle(), actualTilListItem.getTitle());
            assertEquals(tilListItem.getTags(), actualTilListItem.getTags());
            assertEquals(tilListItem.getUserProfileImageUrl(),
                    actualTilListItem.getUserProfileImageUrl());

        }
    }

    @Test
    @DisplayName("유저 TIL 작성글 조회 - 유저가 존재하고 값이 존재하지 않을 경우 - 조회 성공")
    void findUserPost_withValidUserAndNoPost_success() {

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(tilRepository.findUserTils(mockUser.getId(), PAGEABLE)).thenReturn(
                Collections.emptyList());

        UserResponseDTO.GetUserTilsResponseDTO getUserTilsResponseDTO =
                userService.getUserTilsService(mockUser.getId(), PAGEABLE);

        assertNotNull(getUserTilsResponseDTO);
        assertTrue(getUserTilsResponseDTO.getTils().isEmpty());
    }

    @Test
    @DisplayName("유저 TIL 작성글 조회 - 유저가 존재하지 않을 경우 - 조회 실패")
    void findUserPost_withInvalidUser_fail() {

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);

        assertThatThrownBy(() -> userService.getUserTilsService(mockUser.getId(), PAGEABLE))
                .isInstanceOf(UserNotFoundException.class);
    }


    //유저 TIL 기록 조회
    @Test
    @DisplayName("유저 TIL 기록 조회 - 유저가 존재할 경우 - 조회 성공")
    void findUserTilCount_withValidUser_success() {
        // 연도 상수화
        final int TEST_YEAR = 2025;
        final int TEST_MONTH = 5;
        final int TEST_DAY = 25;
        final int TEST_HOUR = 13;
        final int TEST_MINUTE = 29;
        final int TEST_SECOND = 19;
        final int TEST_ZONE_OFFSET = 9;
        final int MOCK_TIL_COUNT = 5;

        OffsetDateTime testDateTime = OffsetDateTime.of(
                TEST_YEAR, TEST_MONTH, TEST_DAY, TEST_HOUR, TEST_MINUTE, TEST_SECOND, 0,
                ZoneOffset.ofHours(TEST_ZONE_OFFSET)
        );

        mockTil.setCreatedAt(testDateTime);

        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenReturn(mockUser);
        when(tilRepository.findAllByUserIdAndYear(mockUser.getId(), TEST_YEAR)).thenReturn(
                Collections.nCopies(MOCK_TIL_COUNT, mockTil)
        );

        GetUserTilCountResponseDTO responseDTO = userService.getUserTilCountService(
                mockUser.getId(), TEST_YEAR);

        assertEquals(MOCK_TIL_COUNT, responseDTO.getTils().getMay().get(TEST_DAY - 1));
    }

    @Test
    @DisplayName("유저 TIL 기록 조회 - 유저가 존재하지 않을 경우 - 조회 실패")
    void findUserTilCount_withInvalidUser_fail() {
        final int TEST_YEAR = 2025;
        when(entityValidator.getValidUserOrThrow(mockUser.getId())).thenThrow(
                UserNotFoundException.class);

        assertThatThrownBy(
                () -> userService.getUserTilCountService(mockUser.getId(), TEST_YEAR)).isInstanceOf(
                UserNotFoundException.class);
    }

    //모듈화 코드
    private void setupWebClient() {
        // POST
        uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.header(anyString(), any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
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
        //서비스 내부의 깃허브App을 모킹한다.

        when(github.getLocal()).thenReturn(githubApp);
        when(githubApp.getClientId()).thenReturn(MOCK_CLIENT_ID);
        when(githubApp.getClientSecret()).thenReturn(MOCK_CLIENT_SECRET);
    }

    private void mockGithubToken() {
        //깃허브 엑세스 토큰 발급을 모킹한다
        GithubResponseDTO.GitHubAccessTokenResponse tokenResponse =
                GithubResponseDTO.GitHubAccessTokenResponse.builder()
                        .access_token(ACCESS_TOKEN)
                        .build();

        when(responseSpec.bodyToMono(GithubResponseDTO.GitHubAccessTokenResponse.class))
                .thenReturn(Mono.just(tokenResponse));
    }

    private void mockEmailAPI(String email) {
        //이메일 조회을 모킹한다
        GithubResponseDTO.GitHubEmailInfo[] emails = {
                GithubResponseDTO.GitHubEmailInfo.builder()
                        .email(email)
                        .primary(true)
                        .verified(true)
                        .build()
        };

        when(getEmailUriSpec.uri(eq(EMAIL_URI))).thenReturn(
                getEmailHeaderSpec);
        when(getEmailHeaderSpec.headers(any())).thenReturn(getEmailHeaderSpec);
        when(getEmailHeaderSpec.retrieve()).thenReturn(getEmailResponseSpec);
        when(getEmailResponseSpec.bodyToMono(GithubResponseDTO.GitHubEmailInfo[].class))
                .thenReturn(Mono.just(emails));
    }

    private void mockUserInfoAPI() {
        //유저정보를 모킹한다
        final String userNickName = "jun";
        final String avatarUrl = "https://avatars.githubusercontent.com/u/123456";

        GithubResponseDTO.GitHubUserInfo userInfo = GithubResponseDTO.GitHubUserInfo.builder()
                .login(userNickName)
                .avatar_url(avatarUrl)
                .build();

        when(getUserUriSpec.uri(eq(USER_SPEC_URI))).thenReturn(getUserHeaderSpec);
        when(getUserHeaderSpec.headers(any())).thenReturn(getUserHeaderSpec);
        when(getUserHeaderSpec.retrieve()).thenReturn(getUserResponseSpec);
        when(getUserResponseSpec.bodyToMono(GithubResponseDTO.GitHubUserInfo.class))
                .thenReturn(Mono.just(userInfo));
    }

    private void mockJwt(long userId) {
        //Jwt 관련 로직을 모킹한다
        when(jwtUtil.generateAccessToken(userId)).thenReturn(JWT_ACCESS_TOKEN);
        when(jwtUtil.generateRefreshToken(userId)).thenReturn(JWT_REFRESH_TOKEN);
    }

}

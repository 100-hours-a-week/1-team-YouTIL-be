package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Security.Encryption.TokenEncryptor;

import static com.youtil.Constants.MockUserConstants.*;
import static com.youtil.Constants.MockGitHubConstants.*;
import static com.youtil.Mock.MockUserBuilder.createMockUser;
import static com.youtil.Mock.MockGitHubBuilder.*;
import com.youtil.Model.User;
import com.youtil.Util.EntityValidator;
import com.youtil.Util.MockUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    @Mock
    private EntityValidator entityValidator;
    @Mock
    private WebClient webClient;
    @Mock
    private TokenEncryptor tokenEncryptor;

    @InjectMocks
    private GithubService githubService;

    private User mockUser;

    @BeforeEach
    void setup() {
        mockUser = createMockUser();
        setupServiceDependencies();
    }

    private void setupServiceDependencies() {
        lenient().when(tokenEncryptor.decrypt(anyString())).thenReturn("valid-github-token");
        ReflectionTestUtils.setField(githubService, "webClient", webClient);
        ReflectionTestUtils.setField(githubService, "tokenEncryptor", tokenEncryptor);
    }

    // ========== 조직 목록 조회 테스트 ==========

    @Test
    @DisplayName("조직 목록 조회 - 유저의 조직 목록이 성공적으로 불러와짐")
    void getOrganizations_withValidUser_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        MockUtil.setupWebClientGetWithMapArrayResponse(webClient, createOrganizationsResponse());

        // when
        GithubResponseDTO.OrganizationResponseDTO result = githubService.getOrganizations(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertEquals(2, result.getOrganizations().size());
        assertEquals(ORG_LOGIN_1, result.getOrganizations().get(0).getOrganization_name());
        assertEquals(ORG_LOGIN_2, result.getOrganizations().get(1).getOrganization_name());
    }

    @Test
    @DisplayName("조직 목록 조회 - 빈 조직 목록이어도 정상적으로 빈 목록을 불러옴")
    void getOrganizations_withEmptyOrganizations_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        MockUtil.setupWebClientGetWithMapArrayResponse(webClient, createEmptyResponse());

        // when
        GithubResponseDTO.OrganizationResponseDTO result = githubService.getOrganizations(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertTrue(result.getOrganizations().isEmpty());
    }

    @Test
    @DisplayName("조직 목록 조회 - 깃허브 토큰이 없거나 유효하지 않은 경우")
    void getOrganizations_withInvalidToken_fail() {
        // given
        mockUser.setGithubToken(null);
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        // when & then
        assertThatThrownBy(() -> githubService.getOrganizations(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GitHub 토큰이 없습니다.");
    }

    @Test
    @DisplayName("조직 목록 조회 - 깃허브 API 호출 제한 초과인 경우")
    void getOrganizations_withApiRateLimit_fail() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.header(anyString(), anyString())).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        lenient().when(getResponseSpec.bodyToMono(eq(Map[].class)))
                .thenThrow(WebClientResponseException.create(403, "API rate limit exceeded", null, null, null));

        // when & then
        assertThatThrownBy(() -> githubService.getOrganizations(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== 레포지토리 목록 조회 테스트 ==========

    @Test
    @DisplayName("레포지토리 목록 조회 - 조직 ID가 제공된 경우 해당 조직의 레포지토리 목록 조회 성공")
    void getRepositories_withOrgId_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        MockUtil.setupWebClientGetWithMapArrayResponse(webClient, createRepositoriesResponse());

        // when
        GithubResponseDTO.RepositoryResponseDTO result = githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, ORG_ID_1);

        // then
        assertNotNull(result);
        assertEquals(1, result.getRepositories().size());
        assertEquals(REPO_NAME_1, result.getRepositories().get(0).getRepositoryName());
    }

    @Test
    @DisplayName("레포지토리 목록 조회 - 조직 ID가 없는 경우 사용자 개인 레포지토리 목록 조회 성공")
    void getRepositories_withoutOrgId_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        MockUtil.setupWebClientGetWithMapArrayResponse(webClient, createPersonalRepositoriesResponse());

        // when
        GithubResponseDTO.RepositoryResponseDTO result = githubService.getUserRepositories(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertEquals(1, result.getRepositories().size());
        assertEquals(PERSONAL_REPO_NAME, result.getRepositories().get(0).getRepositoryName());
    }

    @Test
    @DisplayName("레포지토리 목록 조회 - 빈 레포지토리 목록이어도 정상적으로 빈 목록 출력")
    void getRepositories_withEmptyRepositories_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        MockUtil.setupWebClientGetWithMapArrayResponse(webClient, createEmptyResponse());

        // when
        GithubResponseDTO.RepositoryResponseDTO result = githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, ORG_ID_1);

        // then
        assertNotNull(result);
        assertTrue(result.getRepositories().isEmpty());
    }

    @Test
    @DisplayName("레포지토리 목록 조회 - 존재하지 않는 조직 ID인 경우")
    void getRepositories_withInvalidOrgId_fail() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.header(anyString(), anyString())).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        lenient().when(getResponseSpec.bodyToMono(eq(Map[].class)))
                .thenThrow(WebClientResponseException.create(404, "Organization not found", null, null, null));

        // when & then
        assertThatThrownBy(() -> githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, INVALID_ORG_ID))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("레포지토리 목록 조회 - 깃허브 API 오류")
    void getRepositories_withGitHubApiError_fail() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.header(anyString(), anyString())).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        lenient().when(getResponseSpec.bodyToMono(eq(Map[].class)))
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        // when & then
        assertThatThrownBy(() -> githubService.getUserRepositories(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== 브랜치 목록 조회 테스트 ==========

    @Test
    @DisplayName("브랜치 목록 조회 - 레포지토리 ID로 브랜치 목록 조회 성공")
    void getBranches_withRepoId_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        // Repository 메타데이터와 브랜치 목록을 함께 모킹
        MockUtil.setupWebClientGetWithMultipleResponses(webClient,
                createRepositoryBasic(),
                createBranchesResponse());

        // when
        GithubResponseDTO.BranchResponseDTO result = githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertEquals(2, result.getBranches().size());
        assertEquals(BRANCH_MAIN, result.getBranches().get(0).getName());
        assertEquals(BRANCH_DEVELOP, result.getBranches().get(1).getName());
    }

    @Test
    @DisplayName("브랜치 목록 조회 - 브랜치가 없어도 빈 목록 반환")
    void getBranches_withEmptyBranches_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        // Repository 메타데이터와 빈 브랜치 목록을 함께 모킹
        MockUtil.setupWebClientGetWithMultipleResponses(webClient,
                createRepositoryBasic(),
                createEmptyResponse());

        // when
        GithubResponseDTO.BranchResponseDTO result = githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertTrue(result.getBranches().isEmpty());
    }

    @Test
    @DisplayName("브랜치 목록 조회 - 존재하지 않는 조직/레포 ID인 경우")
    void getBranches_withInvalidRepoId_fail() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        // WebClient 모킹 후 예외 발생 설정
        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.header(anyString(), anyString())).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        lenient().when(getResponseSpec.bodyToMono(eq(Map.class)))
                .thenThrow(WebClientResponseException.create(404, "Repository not found", null, null, null));

        // when & then
        assertThatThrownBy(() -> githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, INVALID_REPO_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("브랜치 목록 조회 - 레포지토리 ID가 제공되지 않는 경우")
    void getBranches_withoutRepoId_fail() {
        // when & then
        assertThatThrownBy(() -> githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, null, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class);
    }
}

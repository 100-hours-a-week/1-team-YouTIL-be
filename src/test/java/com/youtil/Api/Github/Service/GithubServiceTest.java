package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Security.Encryption.TokenEncryptor;

import static com.youtil.Constants.MockUserConstants.*;
import static com.youtil.Constants.MockGitHubConstants.*;
import static com.youtil.Mock.MockUserBuilder.createMockUser;
import com.youtil.Model.User;
import com.youtil.Util.EntityValidator;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class GithubServiceTest {

    @Mock private EntityValidator entityValidator;
    @Mock(lenient = true) private WebClient webClient;
    @Mock(lenient = true) private TokenEncryptor tokenEncryptor;

    @InjectMocks private GithubService githubService;

    private User mockUser;
    private WebClient.RequestBodyUriSpec postUriSpec;
    private WebClient.RequestBodySpec bodySpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;
    private WebClient.RequestHeadersUriSpec getUriSpec;

    @BeforeEach
    void setup() {
        mockUser = createMockUser();
        setupWebClient();
        setupServiceDependencies();
    }

    private void setupServiceDependencies() {
        lenient().when(tokenEncryptor.decrypt(anyString())).thenReturn("valid-github-token");

        ReflectionTestUtils.setField(githubService, "webClient", webClient);
        ReflectionTestUtils.setField(githubService, "tokenEncryptor", tokenEncryptor);
    }

    private void setupWebClient() {
        postUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        bodySpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);
        getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        // 모든 stubbing을 lenient로 설정하고 기본 응답 제공
        lenient().when(webClient.post()).thenReturn(postUriSpec);
        lenient().when(webClient.get()).thenReturn(getUriSpec);

        lenient().when(postUriSpec.uri(ArgumentMatchers.<String>any())).thenReturn(bodySpec);
        lenient().when(postUriSpec.uri(ArgumentMatchers.<java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI>>any())).thenReturn(bodySpec);
        lenient().when(bodySpec.contentType(ArgumentMatchers.<org.springframework.http.MediaType>any())).thenReturn(bodySpec);
        lenient().when(bodySpec.bodyValue(ArgumentMatchers.<Object>any())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);

        lenient().when(getUriSpec.uri(ArgumentMatchers.<String>any())).thenReturn(headersSpec);
        lenient().when(getUriSpec.uri(ArgumentMatchers.<java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI>>any())).thenReturn(headersSpec);
        lenient().when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);

        // 기본 응답 설정 - Owner 정보가 포함된 Repository 메타데이터를 기본값으로 설정
        lenient().when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(createMockRepositoryMetadataWithOwner()));
        lenient().when(responseSpec.bodyToMono(Map[].class)).thenReturn(Mono.just(new Map[0]));
    }

    // ========== 조직 목록 조회 테스트 ==========

    @Test
    @DisplayName("조직 목록 조회 - 유저의 조직 목록이 성공적으로 불러와짐")
    void getOrganizations_withValidUser_success() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map[].class)).thenReturn(Mono.just(createMockOrganizationsResponse()));

        GithubResponseDTO.OrganizationResponseDTO result = githubService.getOrganizations(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        assertNotNull(result);
        assertEquals(2, result.getOrganizations().size());
        assertEquals(ORG_LOGIN_1, result.getOrganizations().get(0).getOrganization_name());
        assertEquals(ORG_LOGIN_2, result.getOrganizations().get(1).getOrganization_name());
    }

    @Test
    @DisplayName("조직 목록 조회 - 빈 조직 목록이어도 정상적으로 빈 목록을 불러옴")
    void getOrganizations_withEmptyOrganizations_success() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map[].class)).thenReturn(Mono.just(new Map[0]));

        GithubResponseDTO.OrganizationResponseDTO result = githubService.getOrganizations(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        assertNotNull(result);
        assertTrue(result.getOrganizations().isEmpty());
    }

    @Test
    @DisplayName("조직 목록 조회 - 깃허브 토큰이 없거나 유효하지 않은 경우")
    void getOrganizations_withInvalidToken_fail() {
        mockUser.setGithubToken(null);
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        assertThatThrownBy(() -> githubService.getOrganizations(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GitHub 토큰이 없습니다.");
    }

    @Test
    @DisplayName("조직 목록 조회 - 깃허브 API 호출 제한 초과인 경우")
    void getOrganizations_withRateLimitExceeded_fail() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map[].class))
                .thenThrow(WebClientResponseException.create(403, "API rate limit exceeded", null, null, null));

        assertThatThrownBy(() -> githubService.getOrganizations(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== 레포지토리 목록 조회 테스트 ==========

    @Test
    @DisplayName("레포지토리 목록 조회 - 조직 ID가 제공된 경우 해당 조직의 레포지토리 목록 조회 성공")
    void getRepositories_withOrgId_success() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map[].class)).thenReturn(Mono.just(createMockRepositoriesResponse()));

        GithubResponseDTO.RepositoryResponseDTO result = githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, ORG_ID_1);

        assertNotNull(result);
        assertEquals(1, result.getRepositories().size());
        assertEquals(REPO_NAME_1, result.getRepositories().get(0).getRepositoryName());
    }

    @Test
    @DisplayName("레포지토리 목록 조회 - 조직 ID가 없는 경우 사용자 개인 레포지토리 목록 조회 성공")
    void getRepositories_withoutOrgId_success() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map[].class)).thenReturn(Mono.just(createMockPersonalRepositoriesResponse()));

        GithubResponseDTO.RepositoryResponseDTO result = githubService.getUserRepositories(MOCK_USER_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        assertNotNull(result);
        assertEquals(1, result.getRepositories().size());
        assertEquals(PERSONAL_REPO_NAME, result.getRepositories().get(0).getRepositoryName());
    }

    @Test
    @DisplayName("레포지토리 목록 조회 - 빈 레포지토리 목록이어도 정상적으로 빈 목록 출력")
    void getRepositories_withEmptyRepositories_success() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map[].class)).thenReturn(Mono.just(new Map[0]));

        GithubResponseDTO.RepositoryResponseDTO result = githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, ORG_ID_1);

        assertNotNull(result);
        assertTrue(result.getRepositories().isEmpty());
    }

    @Test
    @DisplayName("레포지토리 목록 조회 - 존재하지 않는 조직 ID인 경우")
    void getRepositories_withInvalidOrgId_fail() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map[].class))
                .thenThrow(WebClientResponseException.create(404, "Organization not found", null, null, null));

        assertThatThrownBy(() -> githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, INVALID_ORG_ID))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("레포지토리 목록 조회 - 깃허브 API 오류")
    void getRepositories_withGitHubApiError_fail() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map[].class))
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        assertThatThrownBy(() -> githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, ORG_ID_1))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== 브랜치 목록 조회 테스트 ==========

    @Test
    @DisplayName("브랜치 목록 조회 - 레포지토리 ID로 브랜치 목록 조회 성공")
    void getBranches_withRepoId_success() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(createMockRepositoryMetadata()));
        when(responseSpec.bodyToMono(Map[].class))
                .thenReturn(Mono.just(createMockBranchesResponse()));

        GithubResponseDTO.BranchResponseDTO result = githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        assertNotNull(result);
        assertEquals(2, result.getBranches().size());
        assertEquals(BRANCH_MAIN, result.getBranches().get(0).getName());
        assertEquals(BRANCH_DEVELOP, result.getBranches().get(1).getName());
    }

    @Test
    @DisplayName("브랜치 목록 조회 - 조직 ID, 레포 ID가 모두 제공된 경우에 정상 조회 성공")
    void getBranches_withOrgIdAndRepoId_success() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(createMockRepositoryMetadata()));
        when(responseSpec.bodyToMono(Map[].class))
                .thenReturn(Mono.just(createMockSingleBranchResponse()));

        GithubResponseDTO.BranchResponseDTO result = githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        assertNotNull(result);
        assertEquals(1, result.getBranches().size());
        assertEquals(BRANCH_MAIN, result.getBranches().get(0).getName());
    }

    @Test
    @DisplayName("브랜치 목록 조회 - 브랜치가 없어도 빈 목록 반환")
    void getBranches_withEmptyBranches_success() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(createMockRepositoryMetadata()));
        when(responseSpec.bodyToMono(Map[].class))
                .thenReturn(Mono.just(new Map[0]));

        GithubResponseDTO.BranchResponseDTO result = githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE);

        assertNotNull(result);
        assertTrue(result.getBranches().isEmpty());
    }

    @Test
    @DisplayName("브랜치 목록 조회 - 존재하지 않는 조직/레포 ID인 경우")
    void getBranches_withInvalidRepoId_fail() {
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map.class))
                .thenThrow(WebClientResponseException.create(404, "Repository not found", null, null, null));

        assertThatThrownBy(() -> githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, INVALID_REPO_ID, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("브랜치 목록 조회 - 레포지토리 ID가 제공되지 않는 경우")
    void getBranches_withoutRepoId_fail() {
        // 실제 서비스에서 null repositoryId가 들어가면 URL 구성 과정에서 에러가 발생할 것
        assertThatThrownBy(() -> githubService.getBranchesByRepositoryId(
                MOCK_USER_ID, ORG_ID_1, null, GIT_DEFAULT_PAGE, GIT_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== Helper 메서드들  ==========

    private Map[] createMockOrganizationsResponse() {
        return new Map[]{
                createMapOf("id", ORG_ID_1, "login", ORG_LOGIN_1),
                createMapOf("id", ORG_ID_2, "login", ORG_LOGIN_2)
        };
    }

    private Map[] createMockRepositoriesResponse() {
        return new Map[]{
                createMapOf("id", REPO_ID, "name", REPO_NAME_1, "full_name", REPO_FULL_NAME_1)
        };
    }

    private Map[] createMockPersonalRepositoriesResponse() {
        return new Map[]{
                createMapOf("id", PERSONAL_REPO_ID, "name", PERSONAL_REPO_NAME, "full_name", PERSONAL_REPO_FULL_NAME)
        };
    }

    // Repository 메타데이터 (기존 메서드명 유지)
    private Map createMockRepositoryMetadata() {
        return createMapOf(
                "id", REPO_ID,
                "name", REPO_NAME_1,
                "full_name", REPO_FULL_NAME_1,
                "owner", createMapOf(
                        "login", MOCK_USER_NICKNAME,
                        "id", MOCK_USER_ID,
                        "type", "User"
                )
        );
    }

    private Map createMockRepositoryMetadataWithOwner() {
        return createMapOf(
                "id", REPO_ID,
                "name", REPO_NAME_1,
                "full_name", REPO_FULL_NAME_1,
                "description", REPO_DESCRIPTION_1,
                "private", false,
                "owner", createMapOf(
                        "login", MOCK_USER_NICKNAME,
                        "id", MOCK_USER_ID,
                        "type", "User",
                        "avatar_url", MOCK_USER_PROFILE,
                        "html_url", "https://github.com/" + MOCK_USER_NICKNAME
                )
        );
    }

    private Map[] createMockBranchesResponse() {
        return new Map[]{
                createMapOf("name", BRANCH_MAIN),
                createMapOf("name", BRANCH_DEVELOP)
        };
    }

    private Map[] createMockSingleBranchResponse() {
        return new Map[]{
                createMapOf("name", BRANCH_MAIN)
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> createMapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}

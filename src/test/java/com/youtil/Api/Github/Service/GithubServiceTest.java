package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.GithubResponseDTO;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Api.Github.Util.GitHubCacheHelper;
import com.youtil.Exception.GithubException.GitHubExceptions.*;
import com.youtil.Model.User;
import com.youtil.Mock.MockGithubBuilder;
import com.youtil.Util.EntityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

import static com.youtil.Constants.MockGithubConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHub 조직/레포/브랜치 목록 조회 테스트")
class GithubServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private EntityValidator entityValidator;

    @Mock
    private GitHubCacheHelper cacheHelper;

    @Mock
    private GitHubApiUtils gitHubApiUtils;

    @InjectMocks
    private GithubService githubService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = MockGithubBuilder.createMockUserWithGithub();
    }

    @Nested
    @DisplayName("조직 목록 조회 테스트")
    class OrganizationTest {
        @Test
        @DisplayName("성공: 조직 목록이 성공적으로 조회됨")
        void getOrganizations_Success_WithData() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:orgs:%d:page:%d:offset:%d", MOCK_USER_ID, page, offset);

            List<GithubResponseDTO.OrganizationItem> organizations = List.of(
                    GithubResponseDTO.OrganizationItem.builder()
                            .organization_id(123L)
                            .organization_name("카카오")
                            .build(),
                    GithubResponseDTO.OrganizationItem.builder()
                            .organization_id(456L)
                            .organization_name("네이버")
                            .build()
            );

            GithubResponseDTO.OrganizationResponseDTO expectedResponse =
                    GithubResponseDTO.OrganizationResponseDTO.builder()
                            .organizations(organizations)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(2)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("organizations"),
                    eq(GithubResponseDTO.OrganizationResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.OrganizationResponseDTO result =
                    githubService.getOrganizations(MOCK_USER_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrganizations()).hasSize(2);
            assertThat(result.getCurrentPage()).isEqualTo(page);
            assertThat(result.getPageSize()).isEqualTo(offset);
            assertThat(result.getCurrentPageSize()).isEqualTo(2);
            assertThat(result.isHasNext()).isFalse();

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
            verify(cacheHelper).getFromCacheWithFallback(eq(cacheKey), eq("organizations"),
                    eq(GithubResponseDTO.OrganizationResponseDTO.class), any());
        }

        @Test
        @DisplayName("성공: 빈 조직 목록이 정상적으로 조회됨")
        void getOrganizations_Success_EmptyList() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:orgs:%d:page:%d:offset:%d", MOCK_USER_ID, page, offset);

            GithubResponseDTO.OrganizationResponseDTO expectedResponse =
                    GithubResponseDTO.OrganizationResponseDTO.builder()
                            .organizations(Collections.emptyList())
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(0)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("organizations"),
                    eq(GithubResponseDTO.OrganizationResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.OrganizationResponseDTO result =
                    githubService.getOrganizations(MOCK_USER_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getOrganizations()).isEmpty();
            assertThat(result.getCurrentPageSize()).isEqualTo(0);
            assertThat(result.isHasNext()).isFalse();

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("실패: GitHub 토큰이 없거나 유효하지 않은 경우")
        void getOrganizations_Fail_InvalidToken() {
            // Given
            User userWithoutToken = MockGithubBuilder.createMockUserWithoutToken();
            int page = 0;
            int offset = 20;

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(userWithoutToken);
            doThrow(new GitHubTokenException("GitHub 토큰이 유효하지 않습니다."))
                    .when(gitHubApiUtils).validateToken(userWithoutToken);

            // When & Then
            assertThatThrownBy(() -> githubService.getOrganizations(MOCK_USER_ID, page, offset))
                    .isInstanceOf(GitHubTokenException.class)
                    .hasMessage("GitHub 토큰이 유효하지 않습니다.");

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(userWithoutToken);
            verifyNoInteractions(cacheHelper);
        }

        @Test
        @DisplayName("실패: GitHub API 호출 제한 초과")
        void getOrganizations_Fail_RateLimitExceeded() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:orgs:%d:page:%d:offset:%d", MOCK_USER_ID, page, offset);

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);

            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("organizations"),
                    eq(GithubResponseDTO.OrganizationResponseDTO.class), any()))
                    .thenThrow(new GitHubApiException("GitHub API 호출 한도를 초과했습니다.", 429));

            // When & Then
            assertThatThrownBy(() -> githubService.getOrganizations(MOCK_USER_ID, page, offset))
                    .isInstanceOf(GitHubApiException.class)
                    .hasMessage("GitHub API 호출 한도를 초과했습니다.");

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("실패: 사용자를 찾을 수 없는 경우")
        void getOrganizations_Fail_UserNotFound() {
            // Given
            int page = 0;
            int offset = 20;

            when(entityValidator.getValidUserOrThrow(INVALID_USER_ID))
                    .thenThrow(new RuntimeException("사용자를 찾을 수 없습니다."));

            // When & Then
            assertThatThrownBy(() -> githubService.getOrganizations(INVALID_USER_ID, page, offset))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("사용자를 찾을 수 없습니다.");

            verify(entityValidator).getValidUserOrThrow(INVALID_USER_ID);
            verifyNoInteractions(gitHubApiUtils);
            verifyNoInteractions(cacheHelper);
        }

        @Test
        @DisplayName("성공: 페이지네이션이 올바르게 적용됨")
        void getOrganizations_Success_PaginationCorrect() {
            // Given
            int page = 2;
            int offset = 10;
            String cacheKey = String.format("github:orgs:%d:page:%d:offset:%d", MOCK_USER_ID, page, offset);

            List<GithubResponseDTO.OrganizationItem> organizations = List.of(
                    GithubResponseDTO.OrganizationItem.builder()
                            .organization_id(789L)
                            .organization_name("라인")
                            .build()
            );

            GithubResponseDTO.OrganizationResponseDTO expectedResponse =
                    GithubResponseDTO.OrganizationResponseDTO.builder()
                            .organizations(organizations)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(1)
                            .hasNext(true)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("organizations"),
                    eq(GithubResponseDTO.OrganizationResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.OrganizationResponseDTO result =
                    githubService.getOrganizations(MOCK_USER_ID, page, offset);

            // Then
            assertThat(result.getCurrentPage()).isEqualTo(page);
            assertThat(result.getPageSize()).isEqualTo(offset);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getCurrentPageSize()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("레포지토리 목록 조회")
    class OrganizationsTest {
        @Test
        @DisplayName("성공: 조직 ID가 제공된 경우 해당 조직의 레포지토리 목록 조회 성공")
        void getRepositoriesByOrganizationId_Success() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:repos:%d:org:%d:page:%d:offset:%d",
                    MOCK_USER_ID, MOCK_ORGANIZATION_ID, page, offset);

            List<GithubResponseDTO.RepositoryItem> repositories = List.of(
                    GithubResponseDTO.RepositoryItem.builder()
                            .repositoryId(1L)
                            .repositoryName("backend")
                            .build(),
                    GithubResponseDTO.RepositoryItem.builder()
                            .repositoryId(2L)
                            .repositoryName("frontend")
                            .build()
            );

            GithubResponseDTO.RepositoryResponseDTO expectedResponse =
                    GithubResponseDTO.RepositoryResponseDTO.builder()
                            .repositories(repositories)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(2)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("repositories"),
                    eq(GithubResponseDTO.RepositoryResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.RepositoryResponseDTO result =
                    githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, MOCK_ORGANIZATION_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRepositories()).hasSize(2);
            assertThat(result.getCurrentPage()).isEqualTo(page);
            assertThat(result.getPageSize()).isEqualTo(offset);
            assertThat(result.getCurrentPageSize()).isEqualTo(2);
            assertThat(result.isHasNext()).isFalse();

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("성공: 조직 ID가 없는 경우 사용자 개인 레포지토리 목록 조회 성공")
        void getUserRepositories_Success() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:repos:%d:user:page:%d:offset:%d",
                    MOCK_USER_ID, page, offset);

            List<GithubResponseDTO.RepositoryItem> repositories = List.of(
                    GithubResponseDTO.RepositoryItem.builder()
                            .repositoryId(MOCK_REPOSITORY_ID)
                            .repositoryName(MOCK_REPO_NAME)
                            .build()
            );

            GithubResponseDTO.RepositoryResponseDTO expectedResponse =
                    GithubResponseDTO.RepositoryResponseDTO.builder()
                            .repositories(repositories)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(1)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("user_repositories"),
                    eq(GithubResponseDTO.RepositoryResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.RepositoryResponseDTO result =
                    githubService.getUserRepositories(MOCK_USER_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRepositories()).hasSize(1);
            assertThat(result.getRepositories().get(0).getRepositoryName()).isEqualTo(MOCK_REPO_NAME);
            assertThat(result.getCurrentPageSize()).isEqualTo(1);
            assertThat(result.isHasNext()).isFalse();

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("성공: 빈 레포지토리 목록이어도 정상적으로 빈 목록 반환")
        void getRepositories_Success_EmptyList() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:repos:%d:org:%d:page:%d:offset:%d",
                    MOCK_USER_ID, MOCK_ORGANIZATION_ID, page, offset);

            GithubResponseDTO.RepositoryResponseDTO expectedResponse =
                    GithubResponseDTO.RepositoryResponseDTO.builder()
                            .repositories(Collections.emptyList())
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(0)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("repositories"),
                    eq(GithubResponseDTO.RepositoryResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.RepositoryResponseDTO result =
                    githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, MOCK_ORGANIZATION_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getRepositories()).isEmpty();
            assertThat(result.getCurrentPageSize()).isEqualTo(0);
            assertThat(result.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 조직 ID인 경우")
        void getRepositories_Fail_OrganizationNotFound() {
            // Given
            Long invalidOrgId = 99999L;
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:repos:%d:org:%d:page:%d:offset:%d",
                    MOCK_USER_ID, invalidOrgId, page, offset);

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("repositories"),
                    eq(GithubResponseDTO.RepositoryResponseDTO.class), any()))
                    .thenThrow(new GitHubApiException("요청한 리소스를 찾을 수 없습니다.", 404));

            // When & Then
            assertThatThrownBy(() -> githubService.getRepositoriesByOrganizationId(MOCK_USER_ID, invalidOrgId, page, offset))
                    .isInstanceOf(GitHubApiException.class)
                    .hasMessage("요청한 리소스를 찾을 수 없습니다.");

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("실패: GitHub API 오류")
        void getRepositories_Fail_GitHubApiError() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:repos:%d:user:page:%d:offset:%d",
                    MOCK_USER_ID, page, offset);

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("user_repositories"),
                    eq(GithubResponseDTO.RepositoryResponseDTO.class), any()))
                    .thenThrow(new GitHubApiException("GitHub 서버 오류가 발생했습니다.", 500));

            // When & Then
            assertThatThrownBy(() -> githubService.getUserRepositories(MOCK_USER_ID, page, offset))
                    .isInstanceOf(GitHubApiException.class)
                    .hasMessage("GitHub 서버 오류가 발생했습니다.");

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("실패: GitHub 토큰이 유효하지 않은 경우")
        void getRepositories_Fail_InvalidToken() {
            // Given
            User userWithoutToken = MockGithubBuilder.createMockUserWithoutToken();
            int page = 0;
            int offset = 20;

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(userWithoutToken);
            doThrow(new GitHubTokenException("GitHub 토큰이 유효하지 않습니다."))
                    .when(gitHubApiUtils).validateToken(userWithoutToken);

            // When & Then
            assertThatThrownBy(() -> githubService.getUserRepositories(MOCK_USER_ID, page, offset))
                    .isInstanceOf(GitHubTokenException.class)
                    .hasMessage("GitHub 토큰이 유효하지 않습니다.");

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(userWithoutToken);
            verifyNoInteractions(cacheHelper);
        }

        @Test
        @DisplayName("성공: 페이지네이션이 올바르게 적용됨")
        void getRepositories_Success_PaginationCorrect() {
            // Given
            int page = 1;
            int offset = 5;
            String cacheKey = String.format("github:repos:%d:user:page:%d:offset:%d",
                    MOCK_USER_ID, page, offset);

            List<GithubResponseDTO.RepositoryItem> repositories = List.of(
                    GithubResponseDTO.RepositoryItem.builder()
                            .repositoryId(6L)
                            .repositoryName("page-test-repo")
                            .build()
            );

            GithubResponseDTO.RepositoryResponseDTO expectedResponse =
                    GithubResponseDTO.RepositoryResponseDTO.builder()
                            .repositories(repositories)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(1)
                            .hasNext(true)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("user_repositories"),
                    eq(GithubResponseDTO.RepositoryResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.RepositoryResponseDTO result =
                    githubService.getUserRepositories(MOCK_USER_ID, page, offset);

            // Then
            assertThat(result.getCurrentPage()).isEqualTo(page);
            assertThat(result.getPageSize()).isEqualTo(offset);
            assertThat(result.isHasNext()).isTrue();
            assertThat(result.getCurrentPageSize()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("브랜치 목록 조회")
    class BranchesTest {
        @Test
        @DisplayName("성공: 레포지토리 ID로 브랜치 목록 조회 성공")
        void getBranchesByRepositoryId_Success() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:branches:%d:repo:%d:page:%d:offset:%d",
                    MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset);

            List<GithubResponseDTO.BranchItem> branches = List.of(
                    GithubResponseDTO.BranchItem.builder()
                            .name("main")
                            .build(),
                    GithubResponseDTO.BranchItem.builder()
                            .name("develop")
                            .build(),
                    GithubResponseDTO.BranchItem.builder()
                            .name("feature/login")
                            .build()
            );

            GithubResponseDTO.BranchResponseDTO expectedResponse =
                    GithubResponseDTO.BranchResponseDTO.builder()
                            .branches(branches)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(3)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("branches"),
                    eq(GithubResponseDTO.BranchResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.BranchResponseDTO result =
                    githubService.getBranchesByRepositoryId(MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBranches()).hasSize(3);
            assertThat(result.getBranches().get(0).getName()).isEqualTo("main");
            assertThat(result.getCurrentPage()).isEqualTo(page);
            assertThat(result.getPageSize()).isEqualTo(offset);
            assertThat(result.getCurrentPageSize()).isEqualTo(3);
            assertThat(result.isHasNext()).isFalse();

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("성공: 조직 ID, 레포 ID가 모두 제공된 경우에 정상 조회 성공")
        void getBranchesByRepositoryIdWithOrg_Success() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:branches:%d:repo:%d:page:%d:offset:%d",
                    MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset);

            List<GithubResponseDTO.BranchItem> branches = List.of(
                    GithubResponseDTO.BranchItem.builder()
                            .name(MOCK_BRANCH)
                            .build()
            );

            GithubResponseDTO.BranchResponseDTO expectedResponse =
                    GithubResponseDTO.BranchResponseDTO.builder()
                            .branches(branches)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(1)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("branches"),
                    eq(GithubResponseDTO.BranchResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.BranchResponseDTO result =
                    githubService.getBranchesByRepositoryId(MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBranches()).hasSize(1);
            assertThat(result.getBranches().get(0).getName()).isEqualTo(MOCK_BRANCH);
        }

        @Test
        @DisplayName("성공: 개인 레포지토리의 브랜치 목록 조회 성공")
        void getBranchesByRepositoryIdWithoutOrg_Success() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:branches:%d:repo:%d:page:%d:offset:%d",
                    MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset);

            List<GithubResponseDTO.BranchItem> branches = List.of(
                    GithubResponseDTO.BranchItem.builder()
                            .name("main")
                            .build(),
                    GithubResponseDTO.BranchItem.builder()
                            .name("feature/new-feature")
                            .build()
            );

            GithubResponseDTO.BranchResponseDTO expectedResponse =
                    GithubResponseDTO.BranchResponseDTO.builder()
                            .branches(branches)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(2)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("personal_branches"),
                    eq(GithubResponseDTO.BranchResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.BranchResponseDTO result =
                    githubService.getBranchesByRepositoryIdWithoutOrg(MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBranches()).hasSize(2);
            assertThat(result.getCurrentPageSize()).isEqualTo(2);
            assertThat(result.isHasNext()).isFalse();

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("성공: 브랜치가 없어도 빈 목록 반환")
        void getBranches_Success_EmptyList() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:branches:%d:repo:%d:page:%d:offset:%d",
                    MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset);

            GithubResponseDTO.BranchResponseDTO expectedResponse =
                    GithubResponseDTO.BranchResponseDTO.builder()
                            .branches(Collections.emptyList())
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(0)
                            .hasNext(false)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("personal_branches"),
                    eq(GithubResponseDTO.BranchResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.BranchResponseDTO result =
                    githubService.getBranchesByRepositoryIdWithoutOrg(MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getBranches()).isEmpty();
            assertThat(result.getCurrentPageSize()).isEqualTo(0);
            assertThat(result.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 조직/레포 ID인 경우")
        void getBranches_Fail_RepositoryNotFound() {
            // Given
            int page = 0;
            int offset = 20;
            String cacheKey = String.format("github:branches:%d:repo:%d:page:%d:offset:%d",
                    MOCK_USER_ID, INVALID_REPOSITORY_ID, page, offset);

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("branches"),
                    eq(GithubResponseDTO.BranchResponseDTO.class), any()))
                    .thenThrow(new GitHubApiException("해당 ID의 레포지토리를 찾을 수 없습니다: " + INVALID_REPOSITORY_ID, 404));

            // When & Then
            assertThatThrownBy(() -> githubService.getBranchesByRepositoryId(
                    MOCK_USER_ID, MOCK_ORGANIZATION_ID, INVALID_REPOSITORY_ID, page, offset))
                    .isInstanceOf(GitHubApiException.class)
                    .hasMessageContaining("해당 ID의 레포지토리를 찾을 수 없습니다");

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(testUser);
        }

        @Test
        @DisplayName("실패: 레포지토리 ID가 제공되지 않는 경우")
        void getBranches_Fail_RepositoryIdNull() {
            // Given
            int page = 0;
            int offset = 20;

            // When & Then
            assertThatThrownBy(() -> githubService.getBranchesByRepositoryId(
                    MOCK_USER_ID, MOCK_ORGANIZATION_ID, null, page, offset))
                    .isInstanceOf(NullPointerException.class);

            verifyNoInteractions(entityValidator);
            verifyNoInteractions(gitHubApiUtils);
            verifyNoInteractions(cacheHelper);
        }

        @Test
        @DisplayName("실패: GitHub 토큰이 유효하지 않은 경우")
        void getBranches_Fail_InvalidToken() {
            // Given
            User userWithoutToken = MockGithubBuilder.createMockUserWithoutToken();
            int page = 0;
            int offset = 20;

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(userWithoutToken);
            doThrow(new GitHubTokenException("GitHub 토큰이 유효하지 않습니다."))
                    .when(gitHubApiUtils).validateToken(userWithoutToken);

            // When & Then
            assertThatThrownBy(() -> githubService.getBranchesByRepositoryIdWithoutOrg(
                    MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset))
                    .isInstanceOf(GitHubTokenException.class)
                    .hasMessage("GitHub 토큰이 유효하지 않습니다.");

            verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
            verify(gitHubApiUtils).validateToken(userWithoutToken);
            verifyNoInteractions(cacheHelper);
        }

        @Test
        @DisplayName("성공: 페이지네이션이 올바르게 적용됨")
        void getBranches_Success_PaginationCorrect() {
            // Given
            int page = 1;
            int offset = 2;
            String cacheKey = String.format("github:branches:%d:repo:%d:page:%d:offset:%d",
                    MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset);

            List<GithubResponseDTO.BranchItem> branches = List.of(
                    GithubResponseDTO.BranchItem.builder()
                            .name("hotfix/issue-123")
                            .build(),
                    GithubResponseDTO.BranchItem.builder()
                            .name("release/v1.0")
                            .build()
            );

            GithubResponseDTO.BranchResponseDTO expectedResponse =
                    GithubResponseDTO.BranchResponseDTO.builder()
                            .branches(branches)
                            .currentPage(page)
                            .pageSize(offset)
                            .currentPageSize(2)
                            .hasNext(true)
                            .build();

            when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
            doNothing().when(gitHubApiUtils).validateToken(testUser);
            when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("personal_branches"),
                    eq(GithubResponseDTO.BranchResponseDTO.class), any())).thenReturn(expectedResponse);

            // When
            GithubResponseDTO.BranchResponseDTO result =
                    githubService.getBranchesByRepositoryIdWithoutOrg(MOCK_USER_ID, MOCK_REPOSITORY_ID, page, offset);

            // Then
            assertThat(result.getCurrentPage()).isEqualTo(page);
            assertThat(result.getPageSize()).isEqualTo(offset);
            assertThat(result.getCurrentPageSize()).isEqualTo(2);
            assertThat(result.isHasNext()).isTrue();
        }
    }
}

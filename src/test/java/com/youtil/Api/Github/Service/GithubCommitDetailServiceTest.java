package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Api.Github.Util.GitHubCacheHelper;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Model.User;
import com.youtil.Mock.MockGithubBuilder;
import com.youtil.Security.Encryption.TokenEncryptor;
import com.youtil.Util.EntityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static com.youtil.Constants.MockGithubConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHub 커밋 상세 조회 테스트")
class GithubCommitDetailServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private TokenEncryptor tokenEncryptor;

    @Mock
    private EntityValidator entityValidator;

    @Mock
    private GitHubCacheHelper cacheHelper;

    @Mock
    private GitHubApiUtils gitHubApiUtils;

    @InjectMocks
    private GithubCommitDetailService githubCommitDetailService;

    private User testUser;
    private CommitDetailRequestDTO.CommitDetailRequest request;

    @BeforeEach
    void setUp() {
        testUser = MockGithubBuilder.createMockUserWithGithub();

        // 커밋 요약 정보 생성
        List<CommitDetailRequestDTO.CommitSummary> commits = List.of(
                CommitDetailRequestDTO.CommitSummary.builder()
                        .sha("abc123def456")
                        .message("feat: 로그인 기능 구현")
                        .build(),
                CommitDetailRequestDTO.CommitSummary.builder()
                        .sha("def456ghi789")
                        .message("fix: 버그 수정")
                        .build()
        );

        request = CommitDetailRequestDTO.CommitDetailRequest.builder()
                .organizationId(MOCK_ORGANIZATION_ID)
                .repositoryId(MOCK_REPOSITORY_ID)
                .branch(MOCK_BRANCH)
                .commits(commits)
                .build();
    }

    @Test
    @DisplayName("성공: 선택한 커밋들의 상세 정보 조회 성공")
    void getCommitDetails_Success() {
        // Given
        List<CommitDetailResponseDTO.FileDetail> files = List.of(
                CommitDetailResponseDTO.FileDetail.builder()
                        .filepath("src/main/java/com/example/LoginService.java")
                        .latest_code("public class LoginService { ... }")
                        .patches(List.of(
                                CommitDetailResponseDTO.PatchDetail.builder()
                                        .commit_message("feat: 로그인 기능 구현")
                                        .patch("@@ -0,0 +1,10 @@\n+public class LoginService {")
                                        .build()
                        ))
                        .build()
        );

        CommitDetailResponseDTO.CommitDetailResponse expectedResponse =
                CommitDetailResponseDTO.CommitDetailResponse.builder()
                        .username(MOCK_USERNAME)
                        .date(MOCK_DATE)
                        .repo(MOCK_REPO_NAME)
                        .files(files)
                        .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(anyString(), eq("commit_detail"),
                eq(CommitDetailResponseDTO.CommitDetailResponse.class), any())).thenReturn(expectedResponse);

        // When
        CommitDetailResponseDTO.CommitDetailResponse result =
                githubCommitDetailService.getCommitDetails(request, MOCK_USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(MOCK_USERNAME);
        assertThat(result.getDate()).isEqualTo(MOCK_DATE);
        assertThat(result.getRepo()).isEqualTo(MOCK_REPO_NAME);
        assertThat(result.getFiles()).hasSize(1);
        assertThat(result.getFiles().get(0).getFilepath()).contains("LoginService.java");
        assertThat(result.getFiles().get(0).getPatches()).hasSize(1);

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: 필수 파라미터 누락 시 - 레포지토리 ID")
    void getCommitDetails_Fail_MissingRepositoryId() {
        // Given
        CommitDetailRequestDTO.CommitDetailRequest invalidRequest =
                CommitDetailRequestDTO.CommitDetailRequest.builder()
                        .organizationId(MOCK_ORGANIZATION_ID)
                        .repositoryId(null)
                        .branch(MOCK_BRANCH)
                        .commits(request.getCommits())
                        .build();

        // When & Then
        assertThatThrownBy(() -> githubCommitDetailService.getCommitDetails(invalidRequest, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("레포지토리 ID");
    }

    @Test
    @DisplayName("실패: 필수 파라미터 누락 시 - 브랜치명")
    void getCommitDetails_Fail_MissingBranch() {
        // Given
        CommitDetailRequestDTO.CommitDetailRequest invalidRequest =
                CommitDetailRequestDTO.CommitDetailRequest.builder()
                        .organizationId(MOCK_ORGANIZATION_ID)
                        .repositoryId(MOCK_REPOSITORY_ID)
                        .branch(null)
                        .commits(request.getCommits())
                        .build();

        // When & Then
        assertThatThrownBy(() -> githubCommitDetailService.getCommitDetails(invalidRequest, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("브랜치");
    }

    @Test
    @DisplayName("실패: 필수 파라미터 누락 시 - 커밋 목록")
    void getCommitDetails_Fail_MissingCommits() {
        // Given
        CommitDetailRequestDTO.CommitDetailRequest invalidRequest =
                CommitDetailRequestDTO.CommitDetailRequest.builder()
                        .organizationId(MOCK_ORGANIZATION_ID)
                        .repositoryId(MOCK_REPOSITORY_ID)
                        .branch(MOCK_BRANCH)
                        .commits(null)
                        .build();

        // When & Then
        assertThatThrownBy(() -> githubCommitDetailService.getCommitDetails(invalidRequest, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("커밋 목록");
    }

    @Test
    @DisplayName("실패: 선택한 커밋이 존재하지 않는 경우")
    void getCommitDetails_Fail_CommitNotFound() {
        // Given
        List<CommitDetailRequestDTO.CommitSummary> invalidCommits = List.of(
                CommitDetailRequestDTO.CommitSummary.builder()
                        .sha("invalidsha123")
                        .message("존재하지 않는 커밋")
                        .build()
        );

        CommitDetailRequestDTO.CommitDetailRequest invalidRequest =
                CommitDetailRequestDTO.CommitDetailRequest.builder()
                        .organizationId(MOCK_ORGANIZATION_ID)
                        .repositoryId(MOCK_REPOSITORY_ID)
                        .branch(MOCK_BRANCH)
                        .commits(invalidCommits)
                        .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(anyString(), eq("commit_detail"),
                eq(CommitDetailResponseDTO.CommitDetailResponse.class), any()))
                .thenThrow(new RuntimeException(TilMessageCode.GITHUB_API_ERROR.getMessage() + ": 커밋을 찾을 수 없습니다."));

        // When & Then
        assertThatThrownBy(() -> githubCommitDetailService.getCommitDetails(invalidRequest, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("커밋을 찾을 수 없습니다");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: GitHub API 호출 중 오류 발생시")
    void getCommitDetails_Fail_GitHubApiError() {
        // Given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(anyString(), eq("commit_detail"),
                eq(CommitDetailResponseDTO.CommitDetailResponse.class), any()))
                .thenThrow(new RuntimeException("GitHub API 서버 오류가 발생했습니다."));

        // When & Then
        assertThatThrownBy(() -> githubCommitDetailService.getCommitDetails(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("GitHub API 서버 오류가 발생했습니다.");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("성공: 파일 변경 내용과 패치 정보가 올바르게 반환됨")
    void getCommitDetails_Success_FileChangesAndPatches() {
        // Given
        List<CommitDetailResponseDTO.PatchDetail> patches = List.of(
                CommitDetailResponseDTO.PatchDetail.builder()
                        .commit_message("feat: 로그인 기능 구현")
                        .patch("@@ -0,0 +1,10 @@\n+public class LoginService {\n+    // 로그인 로직\n+}")
                        .build(),
                CommitDetailResponseDTO.PatchDetail.builder()
                        .commit_message("fix: 버그 수정")
                        .patch("@@ -5,1 +5,1 @@\n-    // 버그가 있는 코드\n+    // 수정된 코드")
                        .build()
        );

        List<CommitDetailResponseDTO.FileDetail> files = List.of(
                CommitDetailResponseDTO.FileDetail.builder()
                        .filepath("src/main/java/com/example/LoginService.java")
                        .latest_code("public class LoginService {\n    // 수정된 코드\n}")
                        .patches(patches)
                        .build()
        );

        CommitDetailResponseDTO.CommitDetailResponse expectedResponse =
                CommitDetailResponseDTO.CommitDetailResponse.builder()
                        .username(MOCK_USERNAME)
                        .date(MOCK_DATE)
                        .repo(MOCK_REPO_NAME)
                        .files(files)
                        .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(anyString(), eq("commit_detail"),
                eq(CommitDetailResponseDTO.CommitDetailResponse.class), any())).thenReturn(expectedResponse);

        // When
        CommitDetailResponseDTO.CommitDetailResponse result =
                githubCommitDetailService.getCommitDetails(request, MOCK_USER_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFiles()).hasSize(1);

        CommitDetailResponseDTO.FileDetail fileDetail = result.getFiles().get(0);
        assertThat(fileDetail.getFilepath()).contains("LoginService.java");
        assertThat(fileDetail.getLatest_code()).contains("수정된 코드");
        assertThat(fileDetail.getPatches()).hasSize(2);

        assertThat(fileDetail.getPatches().get(0).getCommit_message()).isEqualTo("feat: 로그인 기능 구현");
        assertThat(fileDetail.getPatches().get(1).getCommit_message()).isEqualTo("fix: 버그 수정");
        assertThat(fileDetail.getPatches().get(0).getPatch()).contains("public class LoginService");
        assertThat(fileDetail.getPatches().get(1).getPatch()).contains("수정된 코드");
    }
}

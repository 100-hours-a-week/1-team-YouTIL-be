package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.GitHubRepositorySettingDTO;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Mock.MockGithubBuilder;
import com.youtil.Model.User;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.youtil.Constants.MockGithubConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubRepositorySettingServiceTest {

    @InjectMocks
    private GitHubRepositorySettingService gitHubRepositorySettingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityValidator entityValidator;

    @Mock
    private GitHubApiUtils gitHubApiUtils;

    @Test
    @DisplayName("유효한 개인 레포지토리 설정 성공")
    void setDefaultRepository_PersonalRepository_Success() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = MockGithubBuilder.createPersonalRepoRequest();
        Map<String, Object> repoInfo = createMockRepoInfo();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN)).thenReturn(repoInfo);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRepositoryId()).isEqualTo(MOCK_REPOSITORY_ID);
        assertThat(response.getRepository()).isEqualTo(MOCK_REPO_NAME);
        assertThat(response.getBranch()).isEqualTo(MOCK_BRANCH);
        assertThat(response.getOwner()).isEqualTo(MOCK_OWNER);
        assertThat(response.getOrganizationId()).isNull();
        assertThat(response.getIsConfigured()).isTrue();

        verify(gitHubApiUtils).validateToken(mockUser);
        verify(gitHubApiUtils).getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN);
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("유효한 조직 레포지토리 설정 성공")
    void setDefaultRepository_OrganizationRepository_Success() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = MockGithubBuilder.createOrgRepoRequest();
        Map<String, Object> repoInfo = createMockOrgRepoInfo();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN)).thenReturn(repoInfo);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrganizationId()).isEqualTo(MOCK_ORGANIZATION_ID);
        assertThat(response.getRepositoryId()).isEqualTo(MOCK_REPOSITORY_ID);
        assertThat(response.getRepository()).isEqualTo("org-repo");
        assertThat(response.getOwner()).isEqualTo("test-org");
        assertThat(response.getIsConfigured()).isTrue();
    }

    @Test
    @DisplayName("기존 설정이 있는 경우 새로운 레포지토리로 변경")
    void setDefaultRepository_UpdateExistingConfig_Success() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        mockUser.setUploadRepository("old-config/old-branch");

        GitHubRepositorySettingDTO.SetRepositoryRequest request = MockGithubBuilder.createPersonalRepoRequest();
        Map<String, Object> repoInfo = createMockRepoInfo();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN)).thenReturn(repoInfo);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID);

        // then
        assertThat(response.getRepository()).isEqualTo(MOCK_REPO_NAME);
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("존재하지 않는 레포지토리 ID로 요청 시 예외 발생")
    void setDefaultRepository_InvalidRepositoryId_ThrowsException() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = MockGithubBuilder.createPersonalRepoRequest();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN))
                .thenThrow(new RuntimeException("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다."));

        // when & then
        assertThatThrownBy(() -> gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("GitHub 토큰이 없는 경우 예외 발생")
    void setDefaultRepository_MissingToken_ThrowsException() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithoutToken();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = MockGithubBuilder.createPersonalRepoRequest();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        doThrow(new RuntimeException("GitHub 토큰이 설정되지 않았습니다."))
                .when(gitHubApiUtils).validateToken(mockUser);

        // when & then
        assertThatThrownBy(() -> gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("GitHub 토큰이 설정되지 않았습니다.");
    }

    @Test
    @DisplayName("repositoryId가 null인 경우 GitHub API에서 예외 발생")
    void setDefaultRepository_NullRepositoryId_ThrowsException() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = GitHubRepositorySettingDTO.SetRepositoryRequest.builder()
                .repositoryId(null)
                .branch(MOCK_BRANCH)
                .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(null, MOCK_DECRYPTED_TOKEN))
                .thenThrow(new RuntimeException("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다."));

        // when & then
        assertThatThrownBy(() -> gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("branch가 null인 경우에도 레포지토리 검증까지 진행")
    void setDefaultRepository_NullBranch_ValidatesRepository() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = GitHubRepositorySettingDTO.SetRepositoryRequest.builder()
                .repositoryId(MOCK_REPOSITORY_ID)
                .branch(null)
                .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN))
                .thenThrow(new RuntimeException("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다."));

        // when & then
        assertThatThrownBy(() -> gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("branch가 공백인 경우에도 레포지토리 검증까지 진행")
    void setDefaultRepository_BlankBranch_ValidatesRepository() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = GitHubRepositorySettingDTO.SetRepositoryRequest.builder()
                .repositoryId(MOCK_REPOSITORY_ID)
                .branch("   ")
                .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN))
                .thenThrow(new RuntimeException("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다."));

        // when & then
        assertThatThrownBy(() -> gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 레포지토리를 찾을 수 없거나 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("레포지토리 접근 권한이 없는 경우 예외 발생")
    void setDefaultRepository_AccessDenied_ThrowsException() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = MockGithubBuilder.createPersonalRepoRequest();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN))
                .thenThrow(new RuntimeException("접근 권한이 없습니다."));

        // when & then
        assertThatThrownBy(() -> gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("설정된 레포지토리 조회 성공 - 개인 레포지토리")
    void getDefaultRepository_PersonalRepository_Success() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        Map<String, Object> repoInfo = createMockRepoInfo();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN)).thenReturn(repoInfo);

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.getDefaultRepository(MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsConfigured()).isTrue();
        assertThat(response.getRepositoryId()).isEqualTo(MOCK_REPOSITORY_ID);
        assertThat(response.getRepository()).isEqualTo(MOCK_REPO_NAME);
        assertThat(response.getBranch()).isEqualTo(MOCK_BRANCH);
        assertThat(response.getOwner()).isEqualTo(MOCK_OWNER);
        assertThat(response.getOrganizationId()).isNull();
    }

    @Test
    @DisplayName("설정된 레포지토리 조회 성공 - 조직 레포지토리")
    void getDefaultRepository_OrganizationRepository_Success() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithOrgRepo();
        Map<String, Object> repoInfo = createMockOrgRepoInfo();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN)).thenReturn(repoInfo);

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.getDefaultRepository(MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsConfigured()).isTrue();
        assertThat(response.getOrganizationId()).isEqualTo(MOCK_ORGANIZATION_ID);
        assertThat(response.getRepositoryId()).isEqualTo(MOCK_REPOSITORY_ID);
        assertThat(response.getRepository()).isEqualTo("org-repo");
        assertThat(response.getOwner()).isEqualTo("test-org");
    }

    @Test
    @DisplayName("설정이 없는 사용자의 경우 isConfigured false 반환")
    void getDefaultRepository_NoConfig_ReturnsFalse() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithoutConfig();
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.getDefaultRepository(MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsConfigured()).isFalse();
        verify(gitHubApiUtils, never()).validateToken(any());
    }

    @Test
    @DisplayName("설정된 레포지토리가 삭제된 경우 isConfigured false 반환")
    void getDefaultRepository_DeletedRepository_ReturnsFalse() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN))
                .thenThrow(new RuntimeException("레포지토리를 찾을 수 없습니다"));

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.getDefaultRepository(MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsConfigured()).isFalse();
    }

    @Test
    @DisplayName("잘못된 설정 형식인 경우 isConfigured false 반환")
    void getDefaultRepository_InvalidConfigFormat_ReturnsFalse() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        mockUser.setUploadRepository("invalid-format");
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.getDefaultRepository(MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsConfigured()).isFalse();
    }

    @Test
    @DisplayName("GitHub API 서버 오류 시 예외 발생")
    void setDefaultRepository_GitHubApiError_ThrowsException() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();
        GitHubRepositorySettingDTO.SetRepositoryRequest request = MockGithubBuilder.createPersonalRepoRequest();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN))
                .thenThrow(new RuntimeException("GitHub API 서버 오류가 발생했습니다."));

        // when & then
        assertThatThrownBy(() -> gitHubRepositorySettingService.setDefaultRepository(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("GitHub API 서버 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("조회 시 GitHub 토큰이 만료된 경우 isConfigured false 반환")
    void getDefaultRepository_ExpiredToken_ReturnsFalse() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        doThrow(new RuntimeException("GitHub 토큰이 만료되었습니다."))
                .when(gitHubApiUtils).validateToken(mockUser);

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.getDefaultRepository(MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsConfigured()).isFalse();
    }

    @Test
    @DisplayName("조회 시 GitHub API 장애인 경우 isConfigured false 반환")
    void getDefaultRepository_GitHubApiFailure_ReturnsFalse() {
        // given
        User mockUser = MockGithubBuilder.createMockUserWithGithub();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(gitHubApiUtils.decryptToken(MOCK_ENCRYPTED_TOKEN)).thenReturn(MOCK_DECRYPTED_TOKEN);
        when(gitHubApiUtils.getRepositoryById(MOCK_REPOSITORY_ID, MOCK_DECRYPTED_TOKEN))
                .thenThrow(new RuntimeException("GitHub API 서버 장애"));

        // when
        GitHubRepositorySettingDTO.RepositorySettingResponse response =
                gitHubRepositorySettingService.getDefaultRepository(MOCK_USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsConfigured()).isFalse();
    }

}

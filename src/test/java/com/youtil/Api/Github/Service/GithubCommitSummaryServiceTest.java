package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Api.Github.Util.GitHubApiUtils;
import com.youtil.Api.Github.Util.GitHubCacheHelper;
import com.youtil.Exception.GithubException.GitHubExceptions.*;
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

import java.util.Collections;
import java.util.List;

import static com.youtil.Constants.MockGithubConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHub 커밋 간단 조회 테스트")
class GithubCommitSummaryServiceTest {

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
    private GithubCommitSummaryService githubCommitSummaryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = MockGithubBuilder.createMockUserWithGithub();
    }

    @Test
    @DisplayName("성공: 레포지토리 ID, 브랜치명, 날짜로 해당 날짜의 커밋 목록 조회 성공")
    void getCommitSummary_Success_WithCommits() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        List<CommitSummaryResponseDTO.CommitSummary> commits = List.of(
                CommitSummaryResponseDTO.CommitSummary.builder()
                        .sha("abc123def456789")
                        .commitMessage("feat: 로그인 기능 구현")
                        .build(),
                CommitSummaryResponseDTO.CommitSummary.builder()
                        .sha("def456ghi789abc")
                        .commitMessage("fix: 로그인 버그 수정")
                        .build(),
                CommitSummaryResponseDTO.CommitSummary.builder()
                        .sha("ghi789abc123def")
                        .commitMessage("docs: README 업데이트")
                        .build()
        );

        CommitSummaryResponseDTO.CommitSummaryResponse expectedResponse =
                CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                        .username(MOCK_USERNAME)
                        .date(MOCK_DATE)
                        .repo(MOCK_REPO_NAME)
                        .owner(MOCK_OWNER)
                        .commits(commits)
                        .currentPage(page)
                        .pageSize(offset)
                        .currentPageSize(3)
                        .hasNext(false)
                        .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any())).thenReturn(expectedResponse);

        // When
        CommitSummaryResponseDTO.CommitSummaryResponse result =
                githubCommitSummaryService.getCommitSummary(MOCK_USER_ID, MOCK_ORGANIZATION_ID,
                        MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(MOCK_USERNAME);
        assertThat(result.getDate()).isEqualTo(MOCK_DATE);
        assertThat(result.getRepo()).isEqualTo(MOCK_REPO_NAME);
        assertThat(result.getOwner()).isEqualTo(MOCK_OWNER);
        assertThat(result.getCommits()).hasSize(3);
        assertThat(result.getCurrentPage()).isEqualTo(page);
        assertThat(result.getPageSize()).isEqualTo(offset);
        assertThat(result.getCurrentPageSize()).isEqualTo(3);
        assertThat(result.isHasNext()).isFalse();

        // 커밋 내용 검증
        assertThat(result.getCommits().get(0).getSha()).isEqualTo("abc123def456789");
        assertThat(result.getCommits().get(0).getCommitMessage()).isEqualTo("feat: 로그인 기능 구현");
        assertThat(result.getCommits().get(1).getCommitMessage()).isEqualTo("fix: 로그인 버그 수정");
        assertThat(result.getCommits().get(2).getCommitMessage()).isEqualTo("docs: README 업데이트");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
        verify(cacheHelper).getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any());
    }

    @Test
    @DisplayName("성공: 해당 날짜에 커밋이 없어도 빈 목록 반환 성공")
    void getCommitSummary_Success_NoCommits() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        CommitSummaryResponseDTO.CommitSummaryResponse expectedResponse =
                CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                        .username(MOCK_USERNAME)
                        .date(MOCK_DATE)
                        .repo(MOCK_REPO_NAME)
                        .owner(MOCK_OWNER)
                        .commits(Collections.emptyList())
                        .currentPage(page)
                        .pageSize(offset)
                        .currentPageSize(0)
                        .hasNext(false)
                        .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any())).thenReturn(expectedResponse);

        // When
        CommitSummaryResponseDTO.CommitSummaryResponse result =
                githubCommitSummaryService.getCommitSummary(MOCK_USER_ID, MOCK_ORGANIZATION_ID,
                        MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(MOCK_USERNAME);
        assertThat(result.getDate()).isEqualTo(MOCK_DATE);
        assertThat(result.getRepo()).isEqualTo(MOCK_REPO_NAME);
        assertThat(result.getOwner()).isEqualTo(MOCK_OWNER);
        assertThat(result.getCommits()).isEmpty();
        assertThat(result.getCurrentPageSize()).isEqualTo(0);
        assertThat(result.isHasNext()).isFalse();

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
        verify(cacheHelper).getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any());
    }

    @Test
    @DisplayName("성공: 커밋 목록이 사용자의 것만 필터링되어 반환")
    void getCommitSummary_Success_UserCommitsFiltered() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        // 사용자의 커밋만 포함된 목록 (다른 사용자의 커밋은 필터링됨)
        List<CommitSummaryResponseDTO.CommitSummary> userCommits = List.of(
                CommitSummaryResponseDTO.CommitSummary.builder()
                        .sha("user123commit")
                        .commitMessage("feat: 사용자가 작성한 로그인 기능")
                        .build(),
                CommitSummaryResponseDTO.CommitSummary.builder()
                        .sha("user456commit")
                        .commitMessage("refactor: 코드 리팩토링 by " + MOCK_USERNAME)
                        .build()
        );

        CommitSummaryResponseDTO.CommitSummaryResponse expectedResponse =
                CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                        .username(MOCK_USERNAME)
                        .date(MOCK_DATE)
                        .repo(MOCK_REPO_NAME)
                        .owner(MOCK_OWNER)
                        .commits(userCommits)
                        .currentPage(page)
                        .pageSize(offset)
                        .currentPageSize(2)
                        .hasNext(false)
                        .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any())).thenReturn(expectedResponse);

        // When
        CommitSummaryResponseDTO.CommitSummaryResponse result =
                githubCommitSummaryService.getCommitSummary(MOCK_USER_ID, MOCK_ORGANIZATION_ID,
                        MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(MOCK_USERNAME);
        assertThat(result.getCommits()).hasSize(2);

        // 모든 커밋이 해당 사용자의 것인지 확인
        result.getCommits().forEach(commit -> {
            assertThat(commit.getCommitMessage()).satisfiesAnyOf(
                    msg -> assertThat(msg).contains("사용자가 작성한"),
                    msg -> assertThat(msg).contains("by " + MOCK_USERNAME)
            );
        });

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: 필수 파라미터 누락 시 - 레포지토리 ID")
    void getCommitSummary_Fail_MissingRepositoryId() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%s:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, "null", MOCK_BRANCH, MOCK_DATE, page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new IllegalArgumentException("레포지토리 ID는 필수입니다."));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, null, MOCK_BRANCH, MOCK_DATE, page, offset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("레포지토리 ID는 필수입니다.");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: 필수 파라미터 누락 시 - 브랜치명")
    void getCommitSummary_Fail_MissingBranch() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, "null", MOCK_DATE, page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new IllegalArgumentException("브랜치명은 필수입니다."));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, null, MOCK_DATE, page, offset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("브랜치명은 필수입니다.");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: 필수 파라미터 누락 시 - 날짜")
    void getCommitSummary_Fail_MissingDate() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, "null", page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new IllegalArgumentException("날짜는 필수입니다."));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, null, page, offset))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("날짜는 필수입니다.");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: 날짜 형식이 잘못된 경우 - 잘못된 월")
    void getCommitSummary_Fail_InvalidDateFormat_WrongMonth() {
        // Given
        String invalidDate = "2024-13-15"; // 13월은 존재하지 않음
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, invalidDate, page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new GitHubValidationException("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요."));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, invalidDate, page, offset))
                .isInstanceOf(GitHubValidationException.class)
                .hasMessage("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요.");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: 날짜 형식이 잘못된 경우 - 잘못된 일")
    void getCommitSummary_Fail_InvalidDateFormat_WrongDay() {
        // Given
        String invalidDate = "2024-02-30"; // 2월 30일은 존재하지 않음
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, invalidDate, page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new GitHubValidationException("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요."));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, invalidDate, page, offset))
                .isInstanceOf(GitHubValidationException.class)
                .hasMessage("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요.");
    }

    @Test
    @DisplayName("실패: 날짜 형식이 잘못된 경우 - 완전히 잘못된 형식")
    void getCommitSummary_Fail_InvalidDateFormat_WrongFormat() {
        // Given
        String invalidDate = "20240115"; // YYYY-MM-DD 형식이 아님
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, invalidDate, page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new GitHubValidationException("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요."));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, invalidDate, page, offset))
                .isInstanceOf(GitHubValidationException.class)
                .hasMessage("날짜 형식이 올바르지 않습니다. YYYY-MM-DD 형식으로 입력해주세요.");
    }

    @Test
    @DisplayName("실패: GitHub API 호출 실패 - 서버 오류")
    void getCommitSummary_Fail_GitHubApiServerError() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new GitHubApiException("GitHub API 호출에 실패했습니다", 500));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset))
                .isInstanceOf(GitHubApiException.class)
                .hasMessage("GitHub API 호출에 실패했습니다");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: GitHub API 호출 실패 - 네트워크 오류")
    void getCommitSummary_Fail_GitHubApiNetworkError() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new GitHubApiException("네트워크 연결에 실패했습니다", 503));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset))
                .isInstanceOf(GitHubApiException.class)
                .hasMessage("네트워크 연결에 실패했습니다");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: GitHub API 호출 실패 - 레포지토리를 찾을 수 없음")
    void getCommitSummary_Fail_RepositoryNotFound() {
        // Given
        int page = 0;
        int offset = 20;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, INVALID_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any()))
                .thenThrow(new GitHubApiException("요청한 리소스를 찾을 수 없습니다.", 404));

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, INVALID_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset))
                .isInstanceOf(GitHubApiException.class)
                .hasMessage("요청한 리소스를 찾을 수 없습니다.");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(testUser);
    }

    @Test
    @DisplayName("실패: GitHub 토큰이 유효하지 않은 경우")
    void getCommitSummary_Fail_InvalidToken() {
        // Given
        User userWithoutToken = MockGithubBuilder.createMockUserWithoutToken();
        int page = 0;
        int offset = 20;

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(userWithoutToken);
        doThrow(new GitHubTokenException("GitHub 토큰이 유효하지 않습니다."))
                .when(gitHubApiUtils).validateToken(userWithoutToken);

        // When & Then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, MOCK_ORGANIZATION_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset))
                .isInstanceOf(GitHubTokenException.class)
                .hasMessage("GitHub 토큰이 유효하지 않습니다.");

        verify(entityValidator).getValidUserOrThrow(MOCK_USER_ID);
        verify(gitHubApiUtils).validateToken(userWithoutToken);
        verifyNoInteractions(cacheHelper);
    }

    @Test
    @DisplayName("성공: 페이지네이션이 올바르게 적용됨")
    void getCommitSummary_Success_PaginationCorrect() {
        // Given
        int page = 1;
        int offset = 5;
        String cacheKey = String.format("github:commits:%d:repo:%d:branch:%s:date:%s:page:%d:offset:%d",
                MOCK_USER_ID, MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        List<CommitSummaryResponseDTO.CommitSummary> commits = List.of(
                CommitSummaryResponseDTO.CommitSummary.builder()
                        .sha("page1commit1")
                        .commitMessage("페이지네이션 테스트 커밋 1")
                        .build(),
                CommitSummaryResponseDTO.CommitSummary.builder()
                        .sha("page1commit2")
                        .commitMessage("페이지네이션 테스트 커밋 2")
                        .build()
        );

        CommitSummaryResponseDTO.CommitSummaryResponse expectedResponse =
                CommitSummaryResponseDTO.CommitSummaryResponse.builder()
                        .username(MOCK_USERNAME)
                        .date(MOCK_DATE)
                        .repo(MOCK_REPO_NAME)
                        .owner(MOCK_OWNER)
                        .commits(commits)
                        .currentPage(page)
                        .pageSize(offset)
                        .currentPageSize(2)
                        .hasNext(true) // 다음 페이지가 존재
                        .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(testUser);
        doNothing().when(gitHubApiUtils).validateToken(testUser);
        when(cacheHelper.getFromCacheWithFallback(eq(cacheKey), eq("commit_summary"),
                eq(CommitSummaryResponseDTO.CommitSummaryResponse.class), any())).thenReturn(expectedResponse);

        // When
        CommitSummaryResponseDTO.CommitSummaryResponse result =
                githubCommitSummaryService.getCommitSummary(MOCK_USER_ID, MOCK_ORGANIZATION_ID,
                        MOCK_REPOSITORY_ID, MOCK_BRANCH, MOCK_DATE, page, offset);

        // Then
        assertThat(result.getCurrentPage()).isEqualTo(page);
        assertThat(result.getPageSize()).isEqualTo(offset);
        assertThat(result.getCurrentPageSize()).isEqualTo(2);
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.getCommits()).hasSize(2);
    }
}

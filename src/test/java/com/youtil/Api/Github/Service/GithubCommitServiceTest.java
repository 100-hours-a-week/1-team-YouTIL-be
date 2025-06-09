package com.youtil.Api.Github.Service;

import com.youtil.Api.Github.Dto.CommitSummaryResponseDTO;
import com.youtil.Api.Github.Dto.CommitDetailRequestDTO;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Security.Encryption.TokenEncryptor;

import static com.youtil.Constants.MockUserConstants.*;
import static com.youtil.Constants.MockGitHubConstants.*;
import static com.youtil.Constants.MockTilConstants.TEST_DATE;
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
class GithubCommitServiceTest {

    @Mock private EntityValidator entityValidator;
    @Mock private WebClient webClient;
    @Mock private TokenEncryptor tokenEncryptor;

    @InjectMocks private GithubCommitSummaryService githubCommitSummaryService;
    @InjectMocks private GithubCommitDetailService githubCommitDetailService;

    private User mockUser;
    private WebClient.RequestHeadersUriSpec getUriSpec;
    private WebClient.RequestHeadersSpec headersSpec;
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setup() {
        mockUser = createMockUser();
        setupWebClientMocks();
        setupServiceDependencies();
    }

    private void setupServiceDependencies() {
        lenient().when(tokenEncryptor.decrypt(anyString())).thenReturn("valid-github-token");

        ReflectionTestUtils.setField(githubCommitSummaryService, "webClient", webClient);
        ReflectionTestUtils.setField(githubCommitSummaryService, "tokenEncryptor", tokenEncryptor);
        ReflectionTestUtils.setField(githubCommitDetailService, "webClient", webClient);
        ReflectionTestUtils.setField(githubCommitDetailService, "tokenEncryptor", tokenEncryptor);
    }

    private void setupWebClientMocks() {
        getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(ArgumentMatchers.<String>any())).thenReturn(headersSpec);
        lenient().when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    // ========== 커밋 간단 조회 테스트 ==========

    @Test
    @DisplayName("커밋 간단 조회 - 레포지토리 ID, 브랜치 명, 날짜로 해당 날짜의 커밋 목록 조회 성공")
    void getCommits_withValidParameters_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        setupMultipleWebClientCalls();

        // when
        CommitSummaryResponseDTO.CommitSummaryResponse result = githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, BRANCH_MAIN, TEST_DATE.toString());

        // then
        assertNotNull(result);
        assertEquals(2, result.getCommits().size());
        assertEquals(COMMIT_SHA_1, result.getCommits().get(0).getSha());
    }

    @Test
    @DisplayName("커밋 간단 조회 - 해당 날짜에 커밋이 없어도 빈 목록 반환 성공")
    void getCommits_withNoCommits_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        setupMultipleWebClientCallsWithEmptyCommits();

        // when
        CommitSummaryResponseDTO.CommitSummaryResponse result = githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, BRANCH_MAIN, TEST_DATE.toString());

        // then
        assertNotNull(result);
        assertTrue(result.getCommits().isEmpty());
    }

    @Test
    @DisplayName("커밋 간단 조회 - 커밋 목록이 사용자의 것만 필터링 되어 반환")
    void getCommits_withUserFiltering_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        setupMultipleWebClientCallsWithFilteredCommits();

        // when
        CommitSummaryResponseDTO.CommitSummaryResponse result = githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, BRANCH_MAIN, TEST_DATE.toString());

        // then
        assertNotNull(result);
        assertEquals(1, result.getCommits().size());
    }

    @Test
    @DisplayName("커밋 간단 조회 - 필수 파라미터 누락 시")
    void getCommits_withMissingParameters_fail() {
        // repositoryId null 테스트
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, ORG_ID_1, null, BRANCH_MAIN, TEST_DATE.toString()))
                .isInstanceOf(IllegalArgumentException.class);

        // branch null 테스트
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, null, TEST_DATE.toString()))
                .isInstanceOf(IllegalArgumentException.class);

        // date null 테스트
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, BRANCH_MAIN, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("커밋 간단 조회 - 깃허브 API 호출 실패")
    void getCommits_withGitHubApiFailure_fail() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map.class))
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        // when & then
        assertThatThrownBy(() -> githubCommitSummaryService.getCommitSummary(
                MOCK_USER_ID, ORG_ID_1, REPO_ID, BRANCH_MAIN, TEST_DATE.toString()))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== 선택한 커밋 상세 조회 테스트 ==========

    @Test
    @DisplayName("선택한 커밋 상세 조회 - 커밋 상세 정보 조회 성공")
    void getCommitDetails_withValidCommits_success() {
        // given
        CommitDetailRequestDTO.CommitDetailRequest request = createCommitDetailRequest();
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        setupCommitDetailWebClientCalls();

        // when
        CommitDetailResponseDTO.CommitDetailResponse result =
                githubCommitDetailService.getCommitDetails(request, MOCK_USER_ID);

        // then
        assertNotNull(result);
        assertEquals(2, result.getFiles().size());
        assertEquals(COMMIT_MESSAGE_1, result.getFiles().get(0).getPatches().get(0).getCommit_message());
    }

    @Test
    @DisplayName("선택한 커밋 상세 조회 - 필수 파라미터 누락 시 (커밋 목록)")
    void getCommitDetails_withMissingCommits_fail() {
        // given
        CommitDetailRequestDTO.CommitDetailRequest request = CommitDetailRequestDTO.CommitDetailRequest.builder()
                .repositoryId(REPO_ID)
                .branch(BRANCH_MAIN)
                .commits(null)
                .build();

        // when & then
        assertThatThrownBy(() -> githubCommitDetailService.getCommitDetails(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("선택한 커밋 상세 조회 - 선택한 커밋이 존재하지 않는 경우")
    void getCommitDetails_withNonExistentCommits_fail() {
        // given
        CommitDetailRequestDTO.CommitDetailRequest request = createInvalidCommitDetailRequest();
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(createMockRepositoryMetadataComplete()))
                .thenReturn(Mono.just(createMockUserInfoComplete()))
                .thenThrow(WebClientResponseException.create(404, "Commit not found", null, null, null));

        // when & then
        assertThatThrownBy(() -> githubCommitDetailService.getCommitDetails(request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class);
    }

    // ========== Helper 메서드들 ==========

    private void setupMultipleWebClientCalls() {
        WebClient.RequestHeadersUriSpec userUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersUriSpec repoUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersUriSpec commitsUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        when(webClient.get()).thenReturn(userUriSpec, repoUriSpec, commitsUriSpec);

        setupUserInfoCall(userUriSpec);
        setupRepositoryInfoCall(repoUriSpec);
        setupCommitsCall(commitsUriSpec, createMockCommitsResponse());
    }

    private void setupMultipleWebClientCallsWithEmptyCommits() {
        WebClient.RequestHeadersUriSpec userUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersUriSpec repoUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersUriSpec commitsUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        when(webClient.get()).thenReturn(userUriSpec, repoUriSpec, commitsUriSpec);

        setupUserInfoCall(userUriSpec);
        setupRepositoryInfoCall(repoUriSpec);
        setupCommitsCall(commitsUriSpec, new Map[]{});
    }

    private void setupMultipleWebClientCallsWithFilteredCommits() {
        WebClient.RequestHeadersUriSpec userUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersUriSpec repoUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersUriSpec commitsUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        when(webClient.get()).thenReturn(userUriSpec, repoUriSpec, commitsUriSpec);

        setupUserInfoCall(userUriSpec);
        setupRepositoryInfoCall(repoUriSpec);
        setupCommitsCall(commitsUriSpec, createMockFilteredCommitsResponse());
    }

    private void setupUserInfoCall(WebClient.RequestHeadersUriSpec uriSpec) {
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(uriSpec.uri(eq("https://api.github.com/user"))).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(createMockUserInfoComplete()));
    }

    private void setupRepositoryInfoCall(WebClient.RequestHeadersUriSpec uriSpec) {
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(uriSpec.uri(eq("https://api.github.com/repositories/" + REPO_ID))).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(createMockRepositoryMetadataWithOwner()));
    }

    private void setupCommitsCall(WebClient.RequestHeadersUriSpec uriSpec, Map[] commits) {
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(uriSpec.uri(ArgumentMatchers.<String>any())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map[].class)).thenReturn(Mono.just(commits));
    }

    private void setupCommitDetailWebClientCalls() {
        WebClient.RequestHeadersUriSpec userUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersUriSpec repoUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersUriSpec commitsUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        when(webClient.get()).thenReturn(userUriSpec, repoUriSpec, commitsUriSpec);

        setupUserInfoCall(userUriSpec);
        setupRepositoryInfoCall(repoUriSpec);

        WebClient.RequestHeadersSpec commitsHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec commitsResponseSpec = mock(WebClient.ResponseSpec.class);

        when(commitsUriSpec.uri(ArgumentMatchers.<String>any())).thenReturn(commitsHeadersSpec);
        when(commitsHeadersSpec.header(anyString(), anyString())).thenReturn(commitsHeadersSpec);
        when(commitsHeadersSpec.retrieve()).thenReturn(commitsResponseSpec);
        when(commitsResponseSpec.bodyToMono(Map.class))
                .thenReturn(Mono.just(createMockCommitBasicInfo()));
    }

    private CommitDetailRequestDTO.CommitDetailRequest createCommitDetailRequest() {
        return CommitDetailRequestDTO.CommitDetailRequest.builder()
                .organizationId(ORG_ID_1)
                .repositoryId(REPO_ID)
                .branch(BRANCH_MAIN)
                .commits(List.of(
                        CommitDetailRequestDTO.CommitSummary.builder()
                                .sha(COMMIT_SHA_1)
                                .message(COMMIT_MESSAGE_1)
                                .build()
                ))
                .build();
    }

    private CommitDetailRequestDTO.CommitDetailRequest createInvalidCommitDetailRequest() {
        return CommitDetailRequestDTO.CommitDetailRequest.builder()
                .repositoryId(REPO_ID)
                .branch(BRANCH_MAIN)
                .commits(Arrays.asList(
                        CommitDetailRequestDTO.CommitSummary.builder()
                                .sha(INVALID_COMMIT_SHA)
                                .message(COMMIT_MESSAGE_1)
                                .build()
                ))
                .build();
    }

    // Mock 데이터 생성 메서드들
    private Map createMockUserInfoComplete() {
        return createMapOf(
                "login", MOCK_USER_NICKNAME,
                "id", MOCK_USER_ID,
                "type", "User",
                "name", MOCK_USER_NICKNAME,
                "email", MOCK_USER_EMAIL,
                "avatar_url", MOCK_USER_PROFILE,
                "html_url", "https://github.com/" + MOCK_USER_NICKNAME
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

    private Map createMockRepositoryMetadataComplete() {
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

    private Map[] createMockCommitsResponse() {
        return new Map[]{
                createMapOf(
                        "sha", COMMIT_SHA_1,
                        "commit", createMapOf(
                                "message", COMMIT_MESSAGE_1,
                                "committer", createMapOf("date", TEST_DATE.toString() + "T10:00:00Z")
                        ),
                        "author", createMapOf("login", MOCK_USER_NICKNAME)
                ),
                createMapOf(
                        "sha", COMMIT_SHA_2,
                        "commit", createMapOf(
                                "message", COMMIT_MESSAGE_2,
                                "committer", createMapOf("date", TEST_DATE.toString() + "T11:00:00Z")
                        ),
                        "author", createMapOf("login", MOCK_USER_NICKNAME)
                )
        };
    }

    private Map[] createMockFilteredCommitsResponse() {
        return new Map[]{
                createMapOf(
                        "sha", COMMIT_SHA_1,
                        "commit", createMapOf(
                                "message", COMMIT_MESSAGE_1,
                                "committer", createMapOf("date", TEST_DATE.toString() + "T10:00:00Z")
                        ),
                        "author", createMapOf("login", MOCK_USER_NICKNAME)
                )
        };
    }

    private Map<String, Object> createMockCommitBasicInfo() {
        return createMapOf(
                "sha", COMMIT_SHA_1,
                "commit", createMapOf(
                        "message", COMMIT_MESSAGE_1,
                        "committer", createMapOf("date", TEST_DATE.toString() + "T10:00:00Z")
                ),
                "author", createMapOf("login", MOCK_USER_NICKNAME),
                "files", List.of(
                        createMapOf(
                                "filename", "src/Main.java",
                                "patch", "System.out.println(\"Hello world\");",
                                "status", "modified"
                        ),
                        createMapOf(
                                "filename", "src/Utils.java",
                                "patch", "public void utilMethod() {}",
                                "status", "added"
                        )
                )
        );
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

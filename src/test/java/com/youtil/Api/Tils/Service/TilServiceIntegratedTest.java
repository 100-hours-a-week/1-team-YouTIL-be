package com.youtil.Api.Tils.Service;

import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Api.Community.Service.CommunityService;
import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Tils.Dto.TilAiRequestDTO;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Common.Enums.ErrorMessageCode;
import com.youtil.Common.Enums.TilMessageCode;
import com.youtil.Exception.TilException.TilException.TilAIHealthxception;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("TIL 서비스 통합 테스트")
public class TilServiceIntegratedTest {

    // 테스트 상수 정의
    private static final Long TEST_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long INVALID_USER_ID = 999L;
    private static final Long TEST_TIL_ID = 1L;
    private static final Long INVALID_TIL_ID = 999L;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PROFILE_URL = "https://example.com/profile.jpg";
    private static final String TEST_TIL_TITLE = "테스트 TIL";
    private static final String TEST_TIL_CONTENT = "테스트 내용입니다.";
    private static final String TEST_CATEGORY = "BACKEND";
    private static final String TEST_UPDATED_CATEGORY = "FRONTEND";

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int TEST_TILS_COUNT = 5;
    private static final int INITIAL_COUNT = 0;

    // Enum에서 가져온 실제 메시지들 사용
    private static final String USER_NOT_FOUND_MESSAGE = ErrorMessageCode.USER_NOT_FOUND.getMessage();
    private static final String TIL_ACCESS_DENIED_MESSAGE = TilMessageCode.TIL_ACCESS_DENIED.getMessage();
    private static final String TIL_EDIT_DENIED_MESSAGE = TilMessageCode.TIL_EDIT_DENIED.getMessage();
    private static final String TIL_DELETE_DENIED_MESSAGE = TilMessageCode.TIL_DELETE_DENIED.getMessage();

    private static final String AI_RESPONSE_CONTENT = "# 로그인 기능 구현\n\n로그인 기능을 구현했습니다.";
    private static final String AI_HEALTH_OK = "OK";
    private static final String CONNECTION_FAILED_MESSAGE = "Connection failed";

    private static final String REPO_ID = "123";
    private static final String BRANCH_NAME = "main";
    private static final String COMMIT_TITLE = "로그인 기능 구현";

    private static final List<String> TEST_TAGS = Arrays.asList("Spring", "Java");
    private static final List<String> AI_KEYWORDS = Arrays.asList("로그인", "인증", "스프링");

    // AI Service 관련 Mock
    @MockitoBean
    private WebClient webClient;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    // Repository Mock
    @MockitoBean
    private TilRepository tilRepository;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private EntityValidator entityValidator;

    // Service 인스턴스
    @Autowired
    private TilAiService tilAiService;
    @Autowired
    private TilCommendService tilCommendService;
    @Autowired
    private CommunityService communityService;

    // 테스트 데이터
    private User testUser;
    private Til testTil;
    private TilRequestDTO.CreateAiTilRequest createAiTilRequest;
    private final LocalDate TEST_DATE = LocalDate.now().minusDays(1);

    @BeforeEach
    void setUp() {
        setupWebClientMocks();
        setupTestUser();
        setupTestTil();
        setupCreateAiTilRequest();
    }

    private void setupWebClientMocks() {
        // WebClient 모킹 설정 - GET과 POST 각각 다른 타입 반환
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);

        // POST 요청 체인 설정
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // GET 요청 체인 설정 - 어떤 URI든 허용
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    private void setupTestUser() {
        testUser = mock(User.class);
        when(testUser.getId()).thenReturn(TEST_USER_ID);
        when(testUser.getNickname()).thenReturn(TEST_USERNAME);
        when(testUser.getProfileImageUrl()).thenReturn(TEST_PROFILE_URL);
    }

    private void setupTestTil() {
        testTil = mock(Til.class);
        when(testTil.getId()).thenReturn(TEST_TIL_ID);
        when(testTil.getUser()).thenReturn(testUser);
        when(testTil.getTitle()).thenReturn(TEST_TIL_TITLE);
        when(testTil.getContent()).thenReturn(TEST_TIL_CONTENT);
        when(testTil.getCategory()).thenReturn(TEST_CATEGORY);
        when(testTil.getTag()).thenReturn(TEST_TAGS);
        when(testTil.getIsDisplay()).thenReturn(true);
        when(testTil.getCommitRepository()).thenReturn(REPO_ID);
        when(testTil.getIsUploaded()).thenReturn(true);
        when(testTil.getRecommendCount()).thenReturn(INITIAL_COUNT);
        when(testTil.getVisitedCount()).thenReturn(INITIAL_COUNT);
        when(testTil.getCommentsCount()).thenReturn(INITIAL_COUNT);
        when(testTil.getStatus()).thenReturn(Status.active);
        when(testTil.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(testTil.getUpdatedAt()).thenReturn(OffsetDateTime.now());
    }

    private void setupCreateAiTilRequest() {
        createAiTilRequest = TilRequestDTO.CreateAiTilRequest.builder()
                .repo(REPO_ID)
                .title("AI 생성 TIL")
                .content("AI가 생성한 TIL 내용입니다.")
                .category(TEST_CATEGORY)
                .tags(TEST_TAGS)
                .isShared(true)
                .build();
    }

    @Nested
    @DisplayName("1. AI 서버 연동 기능 테스트")
    class AiServerIntegrationTest {

        @Test
        @DisplayName("AI 서버 헬스 체크 성공")
        void getTilAIHealthStatus_Success() {
            // Given
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(AI_HEALTH_OK));

            // When
            String result = tilAiService.getTilAIHealthStatus();

            // Then
            assertEquals(AI_HEALTH_OK, result);
            verify(webClient).get();
        }

        @Test
        @DisplayName("AI 서버 헬스 체크 실패")
        void getTilAIHealthStatus_Failure() {
            // Given
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenThrow(new RuntimeException(CONNECTION_FAILED_MESSAGE));

            // When & Then
            assertThrows(TilAIHealthxception.class, () -> tilAiService.getTilAIHealthStatus());
        }

        @Test
        @DisplayName("TIL 내용 생성 성공")
        void generateTilContent_Success() {
            // Given
            CommitDetailResponseDTO.CommitDetailResponse commitDetail = createSampleCommitDetail();

            TilAiResponseDTO aiResponse = new TilAiResponseDTO();
            aiResponse.setContent(AI_RESPONSE_CONTENT);
            aiResponse.setKeywords(AI_KEYWORDS);

            when(responseSpec.bodyToMono(TilAiResponseDTO.class)).thenReturn(Mono.just(aiResponse));

            // When
            TilAiResponseDTO result = tilAiService.generateTilContent(commitDetail, Long.valueOf(REPO_ID), BRANCH_NAME, COMMIT_TITLE);

            // Then
            assertNotNull(result);
            assertEquals(AI_RESPONSE_CONTENT, result.getContent());
            assertEquals(AI_KEYWORDS.size(), result.getKeywords().size());
            assertTrue(result.getKeywords().contains("로그인"));

            verify(webClient).post();
        }

        @Test
        @DisplayName("AI 서버 연결 실패")
        void generateTilContent_ConnectionFailure() {
            // Given
            CommitDetailResponseDTO.CommitDetailResponse commitDetail = createSampleCommitDetail();

            when(requestHeadersSpec.retrieve()).thenThrow(
                    new WebClientResponseException(503, "Service Unavailable", null, null, null));

            // When & Then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                    tilAiService.generateTilContent(commitDetail, Long.valueOf(REPO_ID), BRANCH_NAME, COMMIT_TITLE));

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
            assertTrue(exception.getReason().contains("AI 서버와의 연결이 원활하지 않습니다"));
        }

        @Test
        @DisplayName("AI 서버 빈 응답 반환")
        void generateTilContent_EmptyResponse() {
            // Given
            CommitDetailResponseDTO.CommitDetailResponse commitDetail = createSampleCommitDetail();

            when(responseSpec.bodyToMono(TilAiResponseDTO.class)).thenReturn(Mono.empty());

            // When & Then
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                    tilAiService.generateTilContent(commitDetail, Long.valueOf(REPO_ID), BRANCH_NAME, COMMIT_TITLE));

            assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
            assertTrue(exception.getReason().contains("AI 서버에서 유효한 응답을 받지 못했습니다"));
        }
    }

    @Nested
    @DisplayName("2. TIL 생성 기능 테스트")
    class TilCreationTest {

        @Test
        @DisplayName("AI 생성 TIL 저장 성공")
        void createTilFromAi_Success() {
            // Given
            when(entityValidator.getValidUserOrThrow(TEST_USER_ID)).thenReturn(testUser);
            when(tilRepository.save(any(Til.class))).thenAnswer(invocation -> {
                Til savedTil = invocation.getArgument(0);
                Til mockSavedTil = mock(Til.class);
                when(mockSavedTil.getId()).thenReturn(TEST_TIL_ID);
                return mockSavedTil;
            });

            // When
            TilResponseDTO.CreateTilResponse response = tilCommendService.createTilFromAi(createAiTilRequest, TEST_USER_ID);

            // Then
            assertNotNull(response);
            assertEquals(TEST_TIL_ID, response.getTilID());
            verify(entityValidator).getValidUserOrThrow(TEST_USER_ID);
            verify(tilRepository).save(any(Til.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 TIL 생성 시도")
        void createTilFromAi_UserNotFound() {
            // Given
            when(entityValidator.getValidUserOrThrow(INVALID_USER_ID))
                    .thenThrow(new RuntimeException(USER_NOT_FOUND_MESSAGE));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.createTilFromAi(createAiTilRequest, INVALID_USER_ID));

            assertEquals(USER_NOT_FOUND_MESSAGE, exception.getMessage());
            verify(tilRepository, never()).save(any(Til.class));
        }
    }

    @Nested
    @DisplayName("3. TIL 목록 조회 기능 테스트")
    class TilListTest {

        @Test
        @DisplayName("사용자의 TIL 목록 조회 성공")
        void getUserTils_Success() {
            // Given
            Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);

            List<UserResponseDTO.TilListItem> mockTilItems = createMockTilItems(2);

            when(entityValidator.getValidUserOrThrow(TEST_USER_ID)).thenReturn(testUser);
            when(tilRepository.findUserTils(eq(TEST_USER_ID), eq(pageable))).thenReturn(mockTilItems);

            // When
            TilResponseDTO.TilListResponse response = tilCommendService.getUserTils(TEST_USER_ID, DEFAULT_PAGE, DEFAULT_SIZE);

            // Then
            assertNotNull(response);
            assertEquals(2, response.getTils().size());
        }

        @Test
        @DisplayName("특정 날짜에 해당하는 TIL 목록 조회 성공")
        void getUserTilsByDate_Success() {
            // Given
            LocalDateTime startOfDay = TEST_DATE.atStartOfDay();
            LocalDateTime endOfDay = TEST_DATE.atTime(23, 59, 59);
            Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);

            List<UserResponseDTO.TilListItem> mockTilItems = createMockTilItems(1);

            when(entityValidator.getValidUserOrThrow(TEST_USER_ID)).thenReturn(testUser);
            when(tilRepository.findUserTilsByDateRange(eq(TEST_USER_ID), eq(startOfDay), eq(endOfDay), eq(pageable)))
                    .thenReturn(mockTilItems);

            // When
            TilResponseDTO.TilListResponse response = tilCommendService.getUserTilsByDate(TEST_USER_ID, TEST_DATE, DEFAULT_PAGE, DEFAULT_SIZE);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getTils().size());
        }

        @Test
        @DisplayName("해당 날짜에 TIL이 없을 경우 빈 목록 반환")
        void getUserTilsByDate_EmptyResult() {
            // Given
            LocalDateTime startOfDay = TEST_DATE.atStartOfDay();
            LocalDateTime endOfDay = TEST_DATE.atTime(23, 59, 59);
            Pageable pageable = PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE);

            when(entityValidator.getValidUserOrThrow(TEST_USER_ID)).thenReturn(testUser);
            when(tilRepository.findUserTilsByDateRange(eq(TEST_USER_ID), eq(startOfDay), eq(endOfDay), eq(pageable)))
                    .thenReturn(new ArrayList<>());

            // When
            TilResponseDTO.TilListResponse response = tilCommendService.getUserTilsByDate(TEST_USER_ID, TEST_DATE, DEFAULT_PAGE, DEFAULT_SIZE);

            // Then
            assertNotNull(response);
            assertEquals(0, response.getTils().size());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 목록 요청")
        void getUserTils_UserNotFound() {
            // Given
            when(entityValidator.getValidUserOrThrow(INVALID_USER_ID))
                    .thenThrow(new RuntimeException(USER_NOT_FOUND_MESSAGE));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.getUserTils(INVALID_USER_ID, DEFAULT_PAGE, DEFAULT_SIZE));

            assertEquals(USER_NOT_FOUND_MESSAGE, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("4. TIL 상세 조회 기능 테스트")
    class TilDetailTest {

        @Test
        @DisplayName("존재하는 TIL ID로 상세 정보 조회 성공")
        void getTilById_Success() {
            // Given
            when(tilRepository.findById(TEST_TIL_ID)).thenReturn(Optional.of(testTil));

            // When
            TilResponseDTO.TilDetailResponse response = tilCommendService.getTilById(TEST_TIL_ID, TEST_USER_ID);

            // Then
            assertNotNull(response);
            assertEquals(TEST_TIL_ID, response.getId());
            assertEquals(TEST_USER_ID, response.getUserId());
            assertEquals(TEST_TIL_TITLE, response.getTitle());
        }

        @Test
        @DisplayName("존재하지 않는 TIL ID로 조회 시도")
        void getTilById_TilNotFound() {
            // Given
            when(tilRepository.findById(INVALID_TIL_ID)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.getTilById(INVALID_TIL_ID, TEST_USER_ID));

            assertEquals(TilMessageCode.TIL_NOT_FOUND.getMessage(), exception.getMessage());
        }

        @Test
        @DisplayName("삭제된 TIL 조회 시도")
        void getTilById_DeletedTil() {
            // Given
            when(testTil.getStatus()).thenReturn(Status.deactive);
            when(tilRepository.findById(TEST_TIL_ID)).thenReturn(Optional.of(testTil));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.getTilById(TEST_TIL_ID, TEST_USER_ID));

            // 실제 구현에서는 TilMessageCode.TIL_ALREADY_DELETED.getMessage()를 사용할 것으로 예상
            assertEquals(TilMessageCode.TIL_ALREADY_DELETED.getMessage(), exception.getMessage());
        }

        @Test
        @DisplayName("타인의 비공개 TIL 조회 시도")
        void getTilById_AccessDenied() {
            // Given
            when(testTil.getIsDisplay()).thenReturn(false);
            when(tilRepository.findById(TEST_TIL_ID)).thenReturn(Optional.of(testTil));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.getTilById(TEST_TIL_ID, OTHER_USER_ID));

            assertEquals(TIL_ACCESS_DENIED_MESSAGE, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("5. TIL 수정/삭제 기능 테스트")
    class TilUpdateDeleteTest {

        @Test
        @DisplayName("TIL 업데이트 성공")
        void updateTil_Success() {
            // Given
            TilRequestDTO.UpdateTilRequest updateRequest = createUpdateRequest();

            when(tilRepository.findById(TEST_TIL_ID)).thenReturn(Optional.of(testTil));
            when(tilRepository.save(any(Til.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TilResponseDTO.TilDetailResponse response = tilCommendService.updateTil(TEST_TIL_ID, updateRequest, TEST_USER_ID);

            // Then
            assertNotNull(response);
            verify(testTil).setTitle("수정된 제목");
            verify(testTil).setContent("수정된 내용");
            verify(testTil).setCategory(TEST_UPDATED_CATEGORY);
        }

        @Test
        @DisplayName("타인의 TIL 수정 시도")
        void updateTil_AccessDenied() {
            // Given
            TilRequestDTO.UpdateTilRequest updateRequest = createUpdateRequest();
            when(tilRepository.findById(TEST_TIL_ID)).thenReturn(Optional.of(testTil));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.updateTil(TEST_TIL_ID, updateRequest, OTHER_USER_ID));

            assertEquals(TIL_EDIT_DENIED_MESSAGE, exception.getMessage());
        }

        @Test
        @DisplayName("TIL 삭제 성공")
        void deleteTil_Success() {
            // Given
            when(tilRepository.findById(TEST_TIL_ID)).thenReturn(Optional.of(testTil));
            when(tilRepository.save(any(Til.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            tilCommendService.deleteTil(TEST_TIL_ID, TEST_USER_ID);

            // Then
            verify(testTil).setStatus(Status.deactive);
            verify(testTil).setDeletedAt(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("타인의 TIL 삭제 시도")
        void deleteTil_AccessDenied() {
            // Given
            when(tilRepository.findById(TEST_TIL_ID)).thenReturn(Optional.of(testTil));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.deleteTil(TEST_TIL_ID, OTHER_USER_ID));

            assertEquals(TIL_DELETE_DENIED_MESSAGE, exception.getMessage());
        }

        private TilRequestDTO.UpdateTilRequest createUpdateRequest() {
            TilRequestDTO.UpdateTilRequest updateRequest = new TilRequestDTO.UpdateTilRequest();
            updateRequest.setTitle("수정된 제목");
            updateRequest.setContent("수정된 내용");
            updateRequest.setCategory(TEST_UPDATED_CATEGORY);
            return updateRequest;
        }
    }

    @Nested
    @DisplayName("6. 최신 TIL 커뮤니티 조회 기능 테스트")
    class CommunityTest {

        @Test
        @DisplayName("공개된 최신 TIL 목록이 최신순으로 조회 성공")
        void getRecentTils_Success() {
            // Given
            List<Til> testTils = createTestTils();
            when(tilRepository.findRecentPublicTils(any(Pageable.class))).thenReturn(testTils);

            // When
            CommunityResponseDTO.RecentTilListResponse response = communityService.getRecentTils();

            // Then
            assertNotNull(response);
            assertEquals(TEST_TILS_COUNT, response.getTils().size());

            CommunityResponseDTO.RecentTilItem firstItem = response.getTils().get(0);
            assertEquals(TEST_TIL_ID, firstItem.getId());
            assertEquals("테스트 TIL 1", firstItem.getTitle());
            assertEquals(TEST_CATEGORY, firstItem.getCategory());
        }

        @Test
        @DisplayName("TIL이 없는 경우 빈 목록 반환")
        void getRecentTils_EmptyResult() {
            // Given
            when(tilRepository.findRecentPublicTils(any(Pageable.class))).thenReturn(new ArrayList<>());

            // When
            CommunityResponseDTO.RecentTilListResponse response = communityService.getRecentTils();

            // Then
            assertNotNull(response);
            assertEquals(0, response.getTils().size());
        }
    }

    // 테스트용 헬퍼 메서드들
    private CommitDetailResponseDTO.CommitDetailResponse createSampleCommitDetail() {
        List<CommitDetailResponseDTO.PatchDetail> patches = new ArrayList<>();
        patches.add(new CommitDetailResponseDTO.PatchDetail(
                "feat: " + COMMIT_TITLE,
                "@@ -0,0 +1,30 @@ public class LoginController { ... }"
        ));

        List<CommitDetailResponseDTO.FileDetail> files = new ArrayList<>();
        files.add(new CommitDetailResponseDTO.FileDetail(
                "src/main/java/com/example/LoginController.java",
                "public class LoginController { ... }",
                patches
        ));

        return CommitDetailResponseDTO.CommitDetailResponse.builder()
                .username(TEST_USERNAME)
                .date(TEST_DATE.toString())
                .repo("test-repo")
                .files(files)
                .build();
    }

    private List<Til> createTestTils() {
        List<Til> testTils = new ArrayList<>();
        for (int i = 0; i < TEST_TILS_COUNT; i++) {
            Til til = createMockTil(i);
            testTils.add(til);
        }
        return testTils;
    }

    private Til createMockTil(int index) {
        Til til = mock(Til.class);
        when(til.getId()).thenReturn((long) (index + 1));
        when(til.getUser()).thenReturn(testUser);
        when(til.getTitle()).thenReturn("테스트 TIL " + (index + 1));
        when(til.getContent()).thenReturn("테스트 내용입니다. " + (index + 1));
        when(til.getCategory()).thenReturn(TEST_CATEGORY);
        when(til.getTag()).thenReturn(TEST_TAGS);
        when(til.getIsDisplay()).thenReturn(true);
        when(til.getCommitRepository()).thenReturn(REPO_ID);
        when(til.getIsUploaded()).thenReturn(true);
        when(til.getRecommendCount()).thenReturn(index);
        when(til.getVisitedCount()).thenReturn(index * 2);
        when(til.getCommentsCount()).thenReturn(index / 2);
        when(til.getStatus()).thenReturn(Status.active);
        when(til.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(til.getUpdatedAt()).thenReturn(OffsetDateTime.now());
        return til;
    }

    private List<UserResponseDTO.TilListItem> createMockTilItems(int count) {
        List<UserResponseDTO.TilListItem> mockTilItems = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UserResponseDTO.TilListItem item = mock(UserResponseDTO.TilListItem.class);
            mockTilItems.add(item);
        }
        return mockTilItems;
    }
}
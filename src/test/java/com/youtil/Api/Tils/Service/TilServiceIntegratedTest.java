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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("TIL 서비스 통합 테스트")
public class TilServiceIntegratedTest {

    // AI Service 관련 Mock
    @Mock
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
    @Mock
    private TilRepository tilRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EntityValidator entityValidator;

    // Service 인스턴스
    @InjectMocks
    private TilAiService tilAiService;
    @InjectMocks
    private TilCommendService tilCommendService;
    @InjectMocks
    private CommunityService communityService;

    // 테스트 데이터
    private User testUser;
    private Til testTil;
    private TilRequestDTO.CreateAiTilRequest createAiTilRequest;

    @BeforeEach
    void setUp() {
        // AI 서비스 설정
        ReflectionTestUtils.setField(tilAiService, "aiApiUrl", "http://test-ai-server.example.com");

        // WebClient 모킹 설정 수정 - GET과 POST 각각 다른 타입 반환
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);

        // POST 요청 체인 설정
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // GET 요청 체인 설정
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // 테스트 사용자 설정 - Mock 객체 사용
        testUser = mock(User.class);
        when(testUser.getId()).thenReturn(1L);
        when(testUser.getNickname()).thenReturn("testuser");
        when(testUser.getProfileImageUrl()).thenReturn("https://example.com/profile.jpg");

        // 테스트 TIL 설정 - Mock 객체 사용
        testTil = mock(Til.class);
        when(testTil.getId()).thenReturn(1L);
        when(testTil.getUser()).thenReturn(testUser);
        when(testTil.getTitle()).thenReturn("테스트 TIL");
        when(testTil.getContent()).thenReturn("테스트 내용입니다.");
        when(testTil.getCategory()).thenReturn("BACKEND");
        when(testTil.getTag()).thenReturn(Arrays.asList("Spring", "Java"));
        when(testTil.getIsDisplay()).thenReturn(true);
        when(testTil.getCommitRepository()).thenReturn("123");
        when(testTil.getIsUploaded()).thenReturn(true);
        when(testTil.getRecommendCount()).thenReturn(0);
        when(testTil.getVisitedCount()).thenReturn(0);
        when(testTil.getCommentsCount()).thenReturn(0);
        when(testTil.getStatus()).thenReturn(Status.active);
        when(testTil.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(testTil.getUpdatedAt()).thenReturn(OffsetDateTime.now());

        // 테스트 생성 요청 설정
        createAiTilRequest = TilRequestDTO.CreateAiTilRequest.builder()
                .repo("123")
                .title("AI 생성 TIL")
                .content("AI가 생성한 TIL 내용입니다.")
                .category("BACKEND")
                .tags(Arrays.asList("Spring", "Java"))
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
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            // When
            String result = tilAiService.getTilAIHealthStatus();

            // Then
            assertEquals("OK", result);
            verify(webClient).get();
            verify(requestHeadersUriSpec).uri("http://test-ai-server.example.com/health");
        }

        @Test
        @DisplayName("AI 서버 헬스 체크 실패")
        void getTilAIHealthStatus_Failure() {
            // Given
            when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenThrow(new RuntimeException("Connection failed"));

            // When & Then
            assertThrows(TilAIHealthxception.class, () -> tilAiService.getTilAIHealthStatus());
        }

        @Test
        @DisplayName("TIL 내용 생성 성공")
        void generateTilContent_Success() {
            // Given
            CommitDetailResponseDTO.CommitDetailResponse commitDetail = createSampleCommitDetail();

            TilAiResponseDTO aiResponse = new TilAiResponseDTO();
            aiResponse.setContent("# 로그인 기능 구현\n\n로그인 기능을 구현했습니다.");
            aiResponse.setKeywords(Arrays.asList("로그인", "인증", "스프링"));

            when(responseSpec.bodyToMono(TilAiResponseDTO.class)).thenReturn(Mono.just(aiResponse));

            // When
            TilAiResponseDTO result = tilAiService.generateTilContent(commitDetail, 123L, "main", "로그인 기능 구현");

            // Then
            assertNotNull(result);
            assertEquals("# 로그인 기능 구현\n\n로그인 기능을 구현했습니다.", result.getContent());
            assertEquals(3, result.getKeywords().size());
            assertTrue(result.getKeywords().contains("로그인"));
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
                    tilAiService.generateTilContent(commitDetail, 123L, "main", "로그인 기능 구현"));

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
                    tilAiService.generateTilContent(commitDetail, 123L, "main", "로그인 기능 구현"));

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
            when(entityValidator.getValidUserOrThrow(1L)).thenReturn(testUser);
            when(tilRepository.save(any(Til.class))).thenAnswer(invocation -> {
                Til savedTil = invocation.getArgument(0);
                // Mock 객체에 ID 설정
                Til mockSavedTil = mock(Til.class);
                when(mockSavedTil.getId()).thenReturn(1L);
                return mockSavedTil;
            });

            // When
            TilResponseDTO.CreateTilResponse response = tilCommendService.createTilFromAi(createAiTilRequest, 1L);

            // Then
            assertNotNull(response);
            assertEquals(1L, response.getTilID());
            verify(entityValidator).getValidUserOrThrow(1L);
            verify(tilRepository).save(any(Til.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 TIL 생성 시도")
        void createTilFromAi_UserNotFound() {
            // Given
            when(entityValidator.getValidUserOrThrow(999L))
                    .thenThrow(new RuntimeException("해당하는 유저가 존재하지 않습니다."));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.createTilFromAi(createAiTilRequest, 999L));

            assertEquals("해당하는 유저가 존재하지 않습니다.", exception.getMessage());
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
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size);

            // UserResponseDTO.TilListItem Mock 객체 생성
            List<UserResponseDTO.TilListItem> mockTilItems = new ArrayList<>();
            UserResponseDTO.TilListItem item1 = mock(UserResponseDTO.TilListItem.class);
            UserResponseDTO.TilListItem item2 = mock(UserResponseDTO.TilListItem.class);
            mockTilItems.add(item1);
            mockTilItems.add(item2);

            when(entityValidator.getValidUserOrThrow(1L)).thenReturn(testUser);
            when(tilRepository.findUserTils(eq(1L), eq(pageable))).thenReturn(mockTilItems);

            // When
            TilResponseDTO.TilListResponse response = tilCommendService.getUserTils(1L, page, size);

            // Then
            assertNotNull(response);
            assertEquals(2, response.getTils().size());
        }

        @Test
        @DisplayName("특정 날짜에 해당하는 TIL 목록 조회 성공")
        void getUserTilsByDate_Success() {
            // Given
            int page = 0;
            int size = 10;
            LocalDate date = LocalDate.of(2024, 5, 20);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            Pageable pageable = PageRequest.of(page, size);

            List<UserResponseDTO.TilListItem> mockTilItems = new ArrayList<>();
            UserResponseDTO.TilListItem item = mock(UserResponseDTO.TilListItem.class);
            mockTilItems.add(item);

            when(entityValidator.getValidUserOrThrow(1L)).thenReturn(testUser);
            when(tilRepository.findUserTilsByDateRange(eq(1L), eq(startOfDay), eq(endOfDay), eq(pageable)))
                    .thenReturn(mockTilItems);

            // When
            TilResponseDTO.TilListResponse response = tilCommendService.getUserTilsByDate(1L, date, page, size);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getTils().size());
        }

        @Test
        @DisplayName("해당 날짜에 TIL이 없을 경우 빈 목록 반환")
        void getUserTilsByDate_EmptyResult() {
            // Given
            int page = 0;
            int size = 10;
            LocalDate date = LocalDate.of(2024, 5, 20);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            Pageable pageable = PageRequest.of(page, size);

            when(entityValidator.getValidUserOrThrow(1L)).thenReturn(testUser);
            when(tilRepository.findUserTilsByDateRange(eq(1L), eq(startOfDay), eq(endOfDay), eq(pageable)))
                    .thenReturn(new ArrayList<>());

            // When
            TilResponseDTO.TilListResponse response = tilCommendService.getUserTilsByDate(1L, date, page, size);

            // Then
            assertNotNull(response);
            assertEquals(0, response.getTils().size());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 목록 요청")
        void getUserTils_UserNotFound() {
            // Given
            when(entityValidator.getValidUserOrThrow(999L))
                    .thenThrow(new RuntimeException("해당하는 유저가 존재하지 않습니다."));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.getUserTils(999L, 0, 10));

            assertEquals("해당하는 유저가 존재하지 않습니다.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("4. TIL 상세 조회 기능 테스트")
    class TilDetailTest {

        @Test
        @DisplayName("존재하는 TIL ID로 상세 정보 조회 성공")
        void getTilById_Success() {
            // Given
            Long tilId = 1L;
            Long userId = 1L; // 본인

            when(tilRepository.findById(tilId)).thenReturn(Optional.of(testTil));

            // When
            TilResponseDTO.TilDetailResponse response = tilCommendService.getTilById(tilId, userId);

            // Then
            assertNotNull(response);
            assertEquals(tilId, response.getId());
            assertEquals(testUser.getId(), response.getUserId());
            assertEquals("테스트 TIL", response.getTitle());
        }

        @Test
        @DisplayName("존재하지 않는 TIL ID로 조회 시도")
        void getTilById_TilNotFound() {
            // Given
            Long tilId = 999L;
            Long userId = 1L;

            when(tilRepository.findById(tilId)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.getTilById(tilId, userId));

            assertEquals(TilMessageCode.TIL_NOT_FOUND.getMessage(), exception.getMessage());
        }

        @Test
        @DisplayName("삭제된 TIL 조회 시도")
        void getTilById_DeletedTil() {
            // Given
            Long tilId = 1L;
            Long userId = 1L;
            when(testTil.getStatus()).thenReturn(Status.deactive);

            when(tilRepository.findById(tilId)).thenReturn(Optional.of(testTil));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.getTilById(tilId, userId));

            assertEquals(TilMessageCode.TIL_ALREADY_DELETED.getMessage(), exception.getMessage());
        }

        @Test
        @DisplayName("타인의 비공개 TIL 조회 시도")
        void getTilById_AccessDenied() {
            // Given
            Long tilId = 1L;
            Long userId = 2L; // 타인
            when(testTil.getIsDisplay()).thenReturn(false); // 비공개

            when(tilRepository.findById(tilId)).thenReturn(Optional.of(testTil));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.getTilById(tilId, userId));

            assertEquals(TilMessageCode.TIL_ACCESS_DENIED.getMessage(), exception.getMessage());
        }
    }

    @Nested
    @DisplayName("5. TIL 수정/삭제 기능 테스트")
    class TilUpdateDeleteTest {

        @Test
        @DisplayName("TIL 업데이트 성공")
        void updateTil_Success() {
            // Given
            Long tilId = 1L;
            Long userId = 1L;

            TilRequestDTO.UpdateTilRequest updateRequest = new TilRequestDTO.UpdateTilRequest();
            updateRequest.setTitle("수정된 제목");
            updateRequest.setContent("수정된 내용");
            updateRequest.setCategory("FRONTEND");

            when(tilRepository.findById(tilId)).thenReturn(Optional.of(testTil));
            when(tilRepository.save(any(Til.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TilResponseDTO.TilDetailResponse response = tilCommendService.updateTil(tilId, updateRequest, userId);

            // Then
            assertNotNull(response);
            // Mock 객체의 업데이트된 값들을 검증하기 위해 verify 사용
            verify(testTil).setTitle("수정된 제목");
            verify(testTil).setContent("수정된 내용");
            verify(testTil).setCategory("FRONTEND");
        }

        @Test
        @DisplayName("타인의 TIL 수정 시도")
        void updateTil_AccessDenied() {
            // Given
            Long tilId = 1L;
            Long userId = 2L; // 타인

            TilRequestDTO.UpdateTilRequest updateRequest = new TilRequestDTO.UpdateTilRequest();
            updateRequest.setTitle("수정된 제목");

            when(tilRepository.findById(tilId)).thenReturn(Optional.of(testTil));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.updateTil(tilId, updateRequest, userId));

            assertEquals(TilMessageCode.TIL_EDIT_DENIED.getMessage(), exception.getMessage());
        }

        @Test
        @DisplayName("TIL 삭제 성공")
        void deleteTil_Success() {
            // Given
            Long tilId = 1L;
            Long userId = 1L;

            when(tilRepository.findById(tilId)).thenReturn(Optional.of(testTil));
            when(tilRepository.save(any(Til.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            tilCommendService.deleteTil(tilId, userId);

            // Then
            verify(testTil).setStatus(Status.deactive);
            verify(testTil).setDeletedAt(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("타인의 TIL 삭제 시도")
        void deleteTil_AccessDenied() {
            // Given
            Long tilId = 1L;
            Long userId = 2L; // 타인

            when(tilRepository.findById(tilId)).thenReturn(Optional.of(testTil));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    tilCommendService.deleteTil(tilId, userId));

            assertEquals(TilMessageCode.TIL_DELETE_DENIED.getMessage(), exception.getMessage());
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
            assertEquals(5, response.getTils().size());

            // 첫 번째 TIL 확인
            CommunityResponseDTO.RecentTilItem firstItem = response.getTils().get(0);
            assertEquals(1L, firstItem.getId());
            assertEquals("테스트 TIL 1", firstItem.getTitle());
            assertEquals("BACKEND", firstItem.getCategory());
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
                "feat: 로그인 기능 구현",
                "@@ -0,0 +1,30 @@ public class LoginController { ... }"
        ));

        List<CommitDetailResponseDTO.FileDetail> files = new ArrayList<>();
        files.add(new CommitDetailResponseDTO.FileDetail(
                "src/main/java/com/example/LoginController.java",
                "public class LoginController { ... }",
                patches
        ));

        return CommitDetailResponseDTO.CommitDetailResponse.builder()
                .username("testuser")
                .date("2024-05-20")
                .repo("test-repo")
                .files(files)
                .build();
    }

    private List<Til> createTestTils() {
        List<Til> testTils = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Til til = mock(Til.class);
            when(til.getId()).thenReturn((long) (i + 1));
            when(til.getUser()).thenReturn(testUser);
            when(til.getTitle()).thenReturn("테스트 TIL " + (i + 1));
            when(til.getContent()).thenReturn("테스트 내용입니다. " + (i + 1));
            when(til.getCategory()).thenReturn("BACKEND");
            when(til.getTag()).thenReturn(Arrays.asList("Spring", "Java"));
            when(til.getIsDisplay()).thenReturn(true);
            when(til.getCommitRepository()).thenReturn("123");
            when(til.getIsUploaded()).thenReturn(true);
            when(til.getRecommendCount()).thenReturn(i);
            when(til.getVisitedCount()).thenReturn(i * 2);
            when(til.getCommentsCount()).thenReturn(i / 2);
            when(til.getStatus()).thenReturn(Status.active);
            when(til.getCreatedAt()).thenReturn(OffsetDateTime.now());
            when(til.getUpdatedAt()).thenReturn(OffsetDateTime.now());
            testTils.add(til);
        }
        return testTils;
    }
}
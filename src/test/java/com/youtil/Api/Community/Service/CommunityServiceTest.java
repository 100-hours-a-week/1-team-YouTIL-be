package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Constants.CommunityServiceConstant;
import com.youtil.Mock.CommunityMockBuilder;
import com.youtil.Model.Til;
import com.youtil.Model.TilRecommend;
import com.youtil.Model.User;
import com.youtil.Repository.TilRecommendRepository;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.youtil.Constants.MockTilConstants.*;
import static com.youtil.Constants.MockUserConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityService 테스트")
class CommunityServiceTest {

    @InjectMocks
    private CommunityService communityService;

    @Mock
    private TilRepository tilRepository;

    @Mock
    private TilRecommendRepository tilRecommendRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private Pageable pageable;
    private User testUser;
    private Til testTil;
    private TilRecommend testRecommend;

    @BeforeEach
    void setUp() {
        // 페이지네이션 설정 (최신 10개 조회용)
        pageable = PageRequest.of(0, 10);
        
        testUser = CommunityMockBuilder.createUser();
        testTil = CommunityMockBuilder.createPublicTil();
        testRecommend = CommunityMockBuilder.createTilRecommend();
    }

    // ========== 최신 TIL 10개 조회 테스트 ==========

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - 공개된 최신 TIL 목록이 최신순으로 조회 성공")
    void getLatestTils_withPublicTils_success() {
        // given
        List<Til> mockTilList = createTwoTilsWithDifferentDates();
        when(tilRepository.findRecentPublicTils(any(Pageable.class))).thenReturn(mockTilList);

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertEquals(2, result.getTils().size());
        assertEquals(MOCK_TITLE, result.getTils().get(0).getTitle());
        assertEquals(MOCK_USER_NICKNAME, result.getTils().get(0).getNickname());
        assertTrue(result.getTils().get(0).getCreatedAt().isAfter(result.getTils().get(1).getCreatedAt()));

        verify(tilRepository).findRecentPublicTils(any(Pageable.class));
    }

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - 공개 TIL이 없는 경우 빈 목록 반환")
    void getLatestTils_withNoPublicTils_success() {
        // given
        when(tilRepository.findRecentPublicTils(any(Pageable.class))).thenReturn(Collections.emptyList());

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertTrue(result.getTils().isEmpty());
        verify(tilRepository).findRecentPublicTils(any(Pageable.class));
    }

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - 정확히 10개로 제한되어 조회")
    void getLatestTils_limitedToTen_success() {
        // given
        List<Til> mockTilList = createTenTilsWithSequentialDates();
        when(tilRepository.findRecentPublicTils(any(Pageable.class))).thenReturn(mockTilList);

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertEquals(10, result.getTils().size());
        verify(tilRepository).findRecentPublicTils(any(Pageable.class));
    }

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - TIL 항목의 모든 필드가 올바르게 매핑됨")
    void getLatestTils_withCompleteItemData_success() {
        // given
        Til expectedTil = createMockTilWithData(MOCK_TIL_ID, MOCK_TITLE, TEST_DATE.atStartOfDay());
        when(tilRepository.findRecentPublicTils(any(Pageable.class))).thenReturn(List.of(expectedTil));

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertEquals(1, result.getTils().size());

        CommunityResponseDTO.RecentTilItem actualItem = result.getTils().get(0);
        assertEquals(expectedTil.getId(), actualItem.getId());
        assertEquals(expectedTil.getUser().getId(), actualItem.getUserId());
        assertEquals(expectedTil.getUser().getNickname(), actualItem.getNickname());
        assertEquals(expectedTil.getTitle(), actualItem.getTitle());
        assertEquals(expectedTil.getCategory(), actualItem.getCategory());
    }

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - 날짜순 정렬 확인")
    void getLatestTils_withDateSorting_success() {
        // given
        List<Til> sortedTilList = createThreeTilsInDateOrder();
        when(tilRepository.findRecentPublicTils(any(Pageable.class))).thenReturn(sortedTilList);

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertEquals(3, result.getTils().size());

        // 최신순으로 정렬되어 있는지 확인
        List<OffsetDateTime> createdDates = result.getTils().stream()
                .map(CommunityResponseDTO.RecentTilItem::getCreatedAt)
                .collect(Collectors.toList());

        assertThat(createdDates).isSortedAccordingTo(Collections.reverseOrder()); // 최신순 (내림차순)
        assertEquals("최신 TIL", result.getTils().get(0).getTitle());
        assertEquals("이전 TIL", result.getTils().get(1).getTitle());
        assertEquals("가장 이전 TIL", result.getTils().get(2).getTitle());
    }

    @Nested
    @DisplayName("커뮤니티 목록 조회 테스트")
    class GetCommunityTilsTest {

        @Test
        @DisplayName("유효한 카테고리로 요청 시 해당 카테고리 TIL 목록 반환")
        void getCommunityTils_WithValidCategory_Success() {
            // given
            CommunityRequestDTO.CommunityListRequest request = CommunityMockBuilder.createCommunityListRequest();
            List<Til> tilList = CommunityMockBuilder.createTilList(2);
            Pageable pageable = PageRequest.of(CommunityServiceConstant.DEFAULT_PAGE, CommunityServiceConstant.DEFAULT_SIZE);

            given(tilRepository.findRecentPublicTilsByCategory(
                    eq(CommunityServiceConstant.CATEGORY_FULLSTACK.toUpperCase()),
                    eq(pageable)
            )).willReturn(tilList);

            // when
            CommunityResponseDTO.CommunityTilListResponse response = communityService.getCommunityTils(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTils()).hasSize(2);
            assertThat(response.getTils().get(0).getCategory()).isEqualTo(CommunityServiceConstant.TIL_CATEGORY);
            verify(tilRepository).findRecentPublicTilsByCategory(
                    eq(CommunityServiceConstant.CATEGORY_FULLSTACK.toUpperCase()),
                    eq(pageable)
            );
        }

        @Test
        @DisplayName("TIL 목록이 없을 경우 빈 목록 반환")
        void getCommunityTils_WithEmptyResult_ReturnsEmptyList() {
            // given
            CommunityRequestDTO.CommunityListRequest request = CommunityMockBuilder.createCommunityListRequest();
            Pageable pageable = PageRequest.of(CommunityServiceConstant.DEFAULT_PAGE, CommunityServiceConstant.DEFAULT_SIZE);

            given(tilRepository.findRecentPublicTilsByCategory(
                    eq(CommunityServiceConstant.CATEGORY_FULLSTACK.toUpperCase()),
                    eq(pageable)
            )).willReturn(Collections.emptyList());

            // when
            CommunityResponseDTO.CommunityTilListResponse response = communityService.getCommunityTils(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTils()).isEmpty();
            verify(tilRepository).findRecentPublicTilsByCategory(
                    eq(CommunityServiceConstant.CATEGORY_FULLSTACK.toUpperCase()),
                    eq(pageable)
            );
        }
    }

    @Nested
    @DisplayName("커뮤니티 게시글 상세 조회 테스트")
    class GetTilDetailTest {

        @Test
        @DisplayName("유효한 tilId로 요청 시 TIL 상세 정보 반환")
        void getTilDetail_WithValidTilId_Success() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(testTil));
            given(tilRecommendRepository.findByTilIdAndUserId(
                    CommunityServiceConstant.VALID_TIL_ID,
                    CommunityServiceConstant.VALID_USER_ID
            )).willReturn(Optional.empty());

            // when
            CommunityResponseDTO.CommunityPostDetailResponse response =
                    communityService.getTilDetail(CommunityServiceConstant.VALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getPostId()).isEqualTo(CommunityServiceConstant.VALID_TIL_ID);
            assertThat(response.getTitle()).isEqualTo(CommunityServiceConstant.TIL_TITLE);
            assertThat(response.getContent()).isEqualTo(CommunityServiceConstant.TIL_CONTENT);
            assertThat(response.getLiked()).isFalse();

            verify(redisTemplate.opsForZSet()).add(eq(CommunityServiceConstant.REDIS_CHANGED_TILS_KEY),
                    eq(String.valueOf(CommunityServiceConstant.VALID_TIL_ID)), anyDouble());
            verify(redisTemplate.opsForValue()).increment(
                    eq(CommunityServiceConstant.REDIS_TIL_VISIT_COUNT_KEY), eq(1L));
            verify(tilRepository).save(testTil);
        }

        @Test
        @DisplayName("조회 시 조회수 1 증가")
        void getTilDetail_IncrementVisitCount_Success() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            int originalVisitCount = testTil.getVisitedCount();
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(testTil));
            given(tilRecommendRepository.findByTilIdAndUserId(anyLong(), anyLong()))
                    .willReturn(Optional.empty());

            // when
            communityService.getTilDetail(CommunityServiceConstant.VALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID);

            // then
            assertThat(testTil.getVisitedCount()).isEqualTo(originalVisitCount + 1);
            verify(redisTemplate.opsForValue()).increment(
                    eq(CommunityServiceConstant.REDIS_TIL_VISIT_COUNT_KEY), eq(1L));
        }

        @Test
        @DisplayName("존재하지 않는 tilId 요청 시 RuntimeException 발생")
        void getTilDetail_WithInvalidTilId_ThrowsException() {
            // given
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.INVALID_TIL_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    communityService.getTilDetail(CommunityServiceConstant.INVALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityServiceConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("비공개 TIL 요청 시 RuntimeException 발생")
        void getTilDetail_WithPrivateTil_ThrowsException() {
            // given
            Til privateTil = CommunityMockBuilder.createPrivateTil();
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(privateTil));

            // when & then
            assertThatThrownBy(() ->
                    communityService.getTilDetail(CommunityServiceConstant.VALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityServiceConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("삭제된 TIL 요청 시 RuntimeException 발생")
        void getTilDetail_WithDeletedTil_ThrowsException() {
            // given
            Til deletedTil = CommunityMockBuilder.createDeletedTil();
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(deletedTil));

            // when & then
            assertThatThrownBy(() ->
                    communityService.getTilDetail(CommunityServiceConstant.VALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityServiceConstant.ERROR_TIL_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("TIL 좋아요 토글 테스트")
    class ToggleTilLikeTest {

        @Test
        @DisplayName("좋아요하지 않은 TIL에 좋아요 : liked=true, 좋아요수 1 증가")
        void toggleTilLike_AddLike_Success() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            int originalLikeCount = testTil.getRecommendCount();
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(testTil));
            given(tilRecommendRepository.findByTilIdAndUserId(
                    CommunityServiceConstant.VALID_TIL_ID,
                    CommunityServiceConstant.VALID_USER_ID
            )).willReturn(Optional.empty());
            given(tilRecommendRepository.save(any(TilRecommend.class)))
                    .willReturn(testRecommend);

            // when
            CommunityResponseDTO.CommunityLikeResponse response =
                    communityService.toggleTilLike(CommunityServiceConstant.VALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID);

            // then
            assertThat(response.getLiked()).isTrue();
            assertThat(response.getLikeCount()).isEqualTo(originalLikeCount + 1);
            assertThat(testTil.getRecommendCount()).isEqualTo(originalLikeCount + 1);

            verify(tilRecommendRepository).save(any(TilRecommend.class));
            verify(redisTemplate.opsForValue()).increment(
                    eq(CommunityServiceConstant.REDIS_TIL_LIKE_COUNT_KEY), eq(1L));
            verify(tilRepository).save(testTil);
        }

        @Test
        @DisplayName("이미 좋아요한 TIL에 좋아요 : liked=false, 좋아요수 1 감소")
        void toggleTilLike_RemoveLike_Success() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
            int originalLikeCount = testTil.getRecommendCount();
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(testTil));
            given(tilRecommendRepository.findByTilIdAndUserId(
                    CommunityServiceConstant.VALID_TIL_ID,
                    CommunityServiceConstant.VALID_USER_ID
            )).willReturn(Optional.of(testRecommend));

            // when
            CommunityResponseDTO.CommunityLikeResponse response =
                    communityService.toggleTilLike(CommunityServiceConstant.VALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID);

            // then
            assertThat(response.getLiked()).isFalse();
            assertThat(response.getLikeCount()).isEqualTo(originalLikeCount - 1);
            assertThat(testTil.getRecommendCount()).isEqualTo(originalLikeCount - 1);

            verify(tilRecommendRepository).delete(testRecommend);
            verify(redisTemplate.opsForValue()).increment(
                    eq(CommunityServiceConstant.REDIS_TIL_LIKE_COUNT_KEY), eq(-1L));
            verify(tilRepository).save(testTil);
        }

        @Test
        @DisplayName("존재하지 않는 tilId 요청 시 RuntimeException 발생")
        void toggleTilLike_WithInvalidTilId_ThrowsException() {
            // given
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.INVALID_TIL_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    communityService.toggleTilLike(CommunityServiceConstant.INVALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityServiceConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("비공개 TIL에 좋아요 시도 시 RuntimeException 발생")
        void toggleTilLike_WithPrivateTil_ThrowsException() {
            // given
            Til privateTil = CommunityMockBuilder.createPrivateTil();
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(privateTil));

            // when & then
            assertThatThrownBy(() ->
                    communityService.toggleTilLike(CommunityServiceConstant.VALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityServiceConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("삭제된 TIL에 좋아요 시도 시 RuntimeException 발생")
        void toggleTilLike_WithDeletedTil_ThrowsException() {
            // given
            Til deletedTil = CommunityMockBuilder.createDeletedTil();
            given(userRepository.findById(CommunityServiceConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityServiceConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(deletedTil));

            // when & then
            assertThatThrownBy(() ->
                    communityService.toggleTilLike(CommunityServiceConstant.VALID_TIL_ID, CommunityServiceConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityServiceConstant.ERROR_TIL_NOT_FOUND);
        }
    }

    // ========== Helper 메서드들 ==========

    /**
     * 서로 다른 날짜를 가진 2개의 TIL 생성 (최신순 정렬 테스트)
     */
    private List<Til> createTwoTilsWithDifferentDates() {
        return List.of(
                createMockTilWithData(MOCK_TIL_ID, MOCK_TITLE, TEST_DATE.atStartOfDay()),
                createMockTilWithData(MOCK_TIL_ID_2, MOCK_TITLE + " 2", TEST_DATE.minusDays(1).atStartOfDay())
        );
    }

    /**
     * 순차적인 날짜를 가진 10개의 TIL 생성 (페이지네이션 테스트)
     */
    private List<Til> createTenTilsWithSequentialDates() {
        return IntStream.range(0, 10)
                .mapToObj(i -> createMockTilWithData(
                        (long) i,
                        "TIL Title " + i,
                        TEST_DATE.minusDays(i).atStartOfDay()))
                .collect(Collectors.toList());
    }

    /**
     * 날짜순으로 정렬된 3개의 TIL 생성 (정렬 테스트)
     */
    private List<Til> createThreeTilsInDateOrder() {
        return List.of(
                createMockTilWithData(1L, "최신 TIL", TEST_DATE.atStartOfDay()),
                createMockTilWithData(2L, "이전 TIL", TEST_DATE.minusDays(1).atStartOfDay()),
                createMockTilWithData(3L, "가장 이전 TIL", TEST_DATE.minusDays(2).atStartOfDay())
        );
    }

    /**
     * Mock TIL 생성 헬퍼 메서드
     */
    private Til createMockTilWithData(Long id, String title, java.time.LocalDateTime createdAt) {
        Til til = mock(Til.class);
        User user = createMockUser();

        // User Mock 설정
        when(user.getId()).thenReturn(MOCK_USER_ID);
        when(user.getNickname()).thenReturn(MOCK_USER_NICKNAME);
        when(user.getProfileImageUrl()).thenReturn(MOCK_USER_PROFILE);

        // Til Mock 설정
        when(til.getId()).thenReturn(id);
        when(til.getUser()).thenReturn(user);
        when(til.getTitle()).thenReturn(title);
        when(til.getCategory()).thenReturn(MOCK_CATEGORY_BACKEND);
        when(til.getTag()).thenReturn(MOCK_TAGS_SPRING);
        when(til.getRecommendCount()).thenReturn(10);
        when(til.getVisitedCount()).thenReturn(50);
        when(til.getCommentsCount()).thenReturn(3);
        doReturn(createdAt.atOffset(ZoneOffset.ofHours(9))).when(til).getCreatedAt();

        return til;
    }

    /**
     * Mock User 생성 헬퍼 메서드
     */
    private User createMockUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(MOCK_USER_ID);
        when(user.getNickname()).thenReturn(MOCK_USER_NICKNAME);
        when(user.getProfileImageUrl()).thenReturn(MOCK_USER_PROFILE);
        return user;
    }
}
package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Repository.TilRepository;

import static com.youtil.Constants.MockTilConstants.*;
import static com.youtil.Constants.MockUserConstants.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.youtil.Model.Til;
import com.youtil.Model.User;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock private TilRepository tilRepository;

    @InjectMocks private CommunityService communityService;

    private Pageable pageable;

    @BeforeEach
    void setup() {
        // 페이지네이션 설정 (최신 10개 조회용)
        pageable = PageRequest.of(0, 10);
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

package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Repository.TilRepository;

import static com.youtil.Constants.MockTilConstants.*;
import static com.youtil.Constants.MockUserConstants.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import org.assertj.core.util.Lists;
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
        List<Til> mockTilList = createMockTilList();
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
        List<Til> mockTilList = createMockTilListWithTenItems();
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
        Til expectedTil = createMockTil();
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
        List<Til> sortedTilList = createMockSortedTilList();
        when(tilRepository.findRecentPublicTils(any(Pageable.class))).thenReturn(sortedTilList);

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertEquals(3, result.getTils().size());

        // 최신순으로 정렬되어 있는지 확인
        assertTrue(result.getTils().get(0).getCreatedAt().isAfter(result.getTils().get(1).getCreatedAt()));
        assertTrue(result.getTils().get(1).getCreatedAt().isAfter(result.getTils().get(2).getCreatedAt()));

        assertEquals("최신 TIL", result.getTils().get(0).getTitle());
        assertEquals("이전 TIL", result.getTils().get(1).getTitle());
        assertEquals("가장 이전 TIL", result.getTils().get(2).getTitle());
    }

    // ========== Helper 메서드들 ==========

    private List<Til> createMockTilList() {
        return Arrays.asList(
                createMockTilWithData(MOCK_TIL_ID, MOCK_TITLE, TEST_DATE.atStartOfDay()),
                createMockTilWithData(MOCK_TIL_ID_2, MOCK_TITLE + " 2", TEST_DATE.minusDays(1).atStartOfDay())
        );
    }

    private List<Til> createMockTilListWithTenItems() {
        List<Til> tilList = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            tilList.add(createMockTilWithData((long) i, "TIL Title " + i, TEST_DATE.minusDays(i).atStartOfDay()));
        }
        return tilList;
    }

    private Til createMockTil() {
        return createMockTilWithData(MOCK_TIL_ID, MOCK_TITLE, TEST_DATE.atStartOfDay());
    }

    private List<Til> createMockSortedTilList() {
        return Arrays.asList(
                createMockTilWithData(1L, "최신 TIL", TEST_DATE.atStartOfDay()),
                createMockTilWithData(2L, "이전 TIL", TEST_DATE.minusDays(1).atStartOfDay()),
                createMockTilWithData(3L, "가장 이전 TIL", TEST_DATE.minusDays(2).atStartOfDay())
        );
    }

    private Til createMockTilWithData(Long id, String title, java.time.LocalDateTime createdAt) {
        // MockTilBuilder를 사용하되, 필요한 데이터만 설정
        Til til = mock(Til.class);
        com.youtil.Model.User user = mock(com.youtil.Model.User.class);

        when(user.getId()).thenReturn(MOCK_USER_ID);
        when(user.getNickname()).thenReturn(MOCK_USER_NICKNAME);
        when(user.getProfileImageUrl()).thenReturn(MOCK_USER_PROFILE);

        when(til.getId()).thenReturn(id);
        when(til.getUser()).thenReturn(user);
        when(til.getTitle()).thenReturn(title);
        when(til.getCategory()).thenReturn(MOCK_CATEGORY_BACKEND);
        when(til.getTag()).thenReturn(MOCK_TAGS_SPRING);
        when(til.getRecommendCount()).thenReturn(10);
        when(til.getVisitedCount()).thenReturn(50);
        when(til.getCommentsCount()).thenReturn(3);
        doReturn(createdAt.atOffset(java.time.ZoneOffset.ofHours(9))).when(til).getCreatedAt();
        return til;
    }
}

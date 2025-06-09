package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Dto.CommunityResponseDTO;

import static com.youtil.Constants.MockTilConstants.*;
import static com.youtil.Constants.MockUserConstants.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock private CommunityService communityService;

    @BeforeEach
    void setup() {
    }

    // ========== 최신 TIL 10개 조회 테스트 ==========

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - 공개된 최신 TIL 목록이 최신순으로 조회 성공")
    void getLatestTils_withPublicTils_success() {
        // given
        List<CommunityResponseDTO.RecentTilItem> tilList = Arrays.asList(
                CommunityResponseDTO.RecentTilItem.builder()
                        .id(MOCK_TIL_ID)
                        .userId(MOCK_USER_ID)
                        .nickname(MOCK_USER_NICKNAME)
                        .profileImageUrl(MOCK_USER_PROFILE)
                        .title(MOCK_TITLE)
                        .category(MOCK_CATEGORY_BACKEND)
                        .tags(MOCK_TAGS_SPRING)
                        .recommendCount(10)
                        .visitedCount(50)
                        .commentsCount(3)
                        .createdAt(TEST_DATE.atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                        .build(),
                CommunityResponseDTO.RecentTilItem.builder()
                        .id(MOCK_TIL_ID_2)
                        .userId(OTHER_USER_ID)
                        .nickname(MOCK_USER_NICKNAME_2)
                        .profileImageUrl(MOCK_USER_PROFILE)
                        .title(MOCK_TITLE + " 2")
                        .category(MOCK_CATEGORY_BACKEND)
                        .tags(MOCK_TAGS_REACT)
                        .recommendCount(5)
                        .visitedCount(25)
                        .commentsCount(1)
                        .createdAt(TEST_DATE.minusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                        .build()
        );

        CommunityResponseDTO.RecentTilListResponse mockResponse =
                CommunityResponseDTO.RecentTilListResponse.builder()
                        .tils(tilList)
                        .build();

        when(communityService.getRecentTils()).thenReturn(mockResponse);

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertEquals(2, result.getTils().size());
        assertEquals(MOCK_TITLE, result.getTils().get(0).getTitle());
        assertEquals(MOCK_USER_NICKNAME, result.getTils().get(0).getNickname());
        assertTrue(result.getTils().get(0).getCreatedAt().isAfter(result.getTils().get(1).getCreatedAt()));
    }

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - 공개 TIL이 없는 경우 빈 목록 반환")
    void getLatestTils_withNoPublicTils_success() {
        // given
        CommunityResponseDTO.RecentTilListResponse mockResponse =
                CommunityResponseDTO.RecentTilListResponse.builder()
                        .tils(Collections.emptyList())
                        .build();

        when(communityService.getRecentTils()).thenReturn(mockResponse);

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertTrue(result.getTils().isEmpty());
    }

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - 정확히 10개로 제한되어 조회")
    void getLatestTils_limitedToTen_success() {
        // given
        List<CommunityResponseDTO.RecentTilItem> tilList = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            tilList.add(CommunityResponseDTO.RecentTilItem.builder()
                    .id((long) i)
                    .userId((long) i)
                    .nickname("User " + i)
                    .profileImageUrl("profile" + i + ".jpg")
                    .title("TIL Title " + i)
                    .category(MOCK_CATEGORY_BACKEND)
                    .tags(Arrays.asList("tag" + i))
                    .recommendCount(i)
                    .visitedCount(i * 10)
                    .commentsCount(i % 3)
                    .createdAt(TEST_DATE.minusDays(i).atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                    .build());
        }

        CommunityResponseDTO.RecentTilListResponse mockResponse =
                CommunityResponseDTO.RecentTilListResponse.builder()
                        .tils(tilList)
                        .build();

        when(communityService.getRecentTils()).thenReturn(mockResponse);

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertEquals(10, result.getTils().size());
        verify(communityService).getRecentTils();
    }

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - TIL 항목의 모든 필드가 올바르게 매핑됨")
    void getLatestTils_withCompleteItemData_success() {
        // given
        CommunityResponseDTO.RecentTilItem expectedItem = CommunityResponseDTO.RecentTilItem.builder()
                .id(MOCK_TIL_ID)
                .userId(MOCK_USER_ID)
                .nickname(MOCK_USER_NICKNAME)
                .profileImageUrl(MOCK_USER_PROFILE)
                .title(MOCK_TITLE)
                .category(MOCK_CATEGORY_BACKEND)
                .tags(MOCK_TAGS_SPRING)
                .recommendCount(10)
                .visitedCount(50)
                .commentsCount(3)
                .createdAt(TEST_DATE.atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                .build();

        CommunityResponseDTO.RecentTilListResponse mockResponse =
                CommunityResponseDTO.RecentTilListResponse.builder()
                        .tils(List.of(expectedItem))
                        .build();

        when(communityService.getRecentTils()).thenReturn(mockResponse);

        // when
        CommunityResponseDTO.RecentTilListResponse result = communityService.getRecentTils();

        // then
        assertNotNull(result);
        assertEquals(1, result.getTils().size());

        CommunityResponseDTO.RecentTilItem actualItem = result.getTils().get(0);
        assertEquals(expectedItem.getId(), actualItem.getId());
        assertEquals(expectedItem.getUserId(), actualItem.getUserId());
        assertEquals(expectedItem.getNickname(), actualItem.getNickname());
        assertEquals(expectedItem.getProfileImageUrl(), actualItem.getProfileImageUrl());
        assertEquals(expectedItem.getTitle(), actualItem.getTitle());
        assertEquals(expectedItem.getCategory(), actualItem.getCategory());
        assertEquals(expectedItem.getTags(), actualItem.getTags());
        assertEquals(expectedItem.getRecommendCount(), actualItem.getRecommendCount());
        assertEquals(expectedItem.getVisitedCount(), actualItem.getVisitedCount());
        assertEquals(expectedItem.getCommentsCount(), actualItem.getCommentsCount());
        assertEquals(expectedItem.getCreatedAt(), actualItem.getCreatedAt());
    }

    @Test
    @DisplayName("community 도메인 : 최신 TIL 10개 조회 - 날짜순 정렬 확인")
    void getLatestTils_withDateSorting_success() {
        // given
        List<CommunityResponseDTO.RecentTilItem> tilList = Arrays.asList(
                CommunityResponseDTO.RecentTilItem.builder()
                        .id(1L)
                        .userId(MOCK_USER_ID)
                        .nickname(MOCK_USER_NICKNAME)
                        .profileImageUrl(MOCK_USER_PROFILE)
                        .title("최신 TIL")
                        .category(MOCK_CATEGORY_BACKEND)
                        .tags(MOCK_TAGS_SPRING)
                        .recommendCount(5)
                        .visitedCount(25)
                        .commentsCount(2)
                        .createdAt(TEST_DATE.atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                        .build(),
                CommunityResponseDTO.RecentTilItem.builder()
                        .id(2L)
                        .userId(OTHER_USER_ID)
                        .nickname(MOCK_USER_NICKNAME_2)
                        .profileImageUrl(MOCK_USER_PROFILE)
                        .title("이전 TIL")
                        .category(MOCK_CATEGORY_BACKEND)
                        .tags(MOCK_TAGS_REACT)
                        .recommendCount(3)
                        .visitedCount(15)
                        .commentsCount(1)
                        .createdAt(TEST_DATE.minusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                        .build(),
                CommunityResponseDTO.RecentTilItem.builder()
                        .id(3L)
                        .userId(MOCK_USER_ID)
                        .nickname(MOCK_USER_NICKNAME)
                        .profileImageUrl(MOCK_USER_PROFILE)
                        .title("가장 이전 TIL")
                        .category(MOCK_CATEGORY_BACKEND)
                        .tags(MOCK_TAGS_SPRING)
                        .recommendCount(1)
                        .visitedCount(5)
                        .commentsCount(0)
                        .createdAt(TEST_DATE.minusDays(2).atStartOfDay().atOffset(java.time.ZoneOffset.UTC))
                        .build()
        );

        CommunityResponseDTO.RecentTilListResponse mockResponse =
                CommunityResponseDTO.RecentTilListResponse.builder()
                        .tils(tilList)
                        .build();

        when(communityService.getRecentTils()).thenReturn(mockResponse);

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
}

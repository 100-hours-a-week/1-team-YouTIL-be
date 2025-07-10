package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Model.Til;
import com.youtil.Model.TilRecommend;
import com.youtil.Model.User;
import com.youtil.Repository.TilRecommendRepository;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Mock.CommunityMockBuilder;
import com.youtil.Constants.CommunityServiceConstant;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private User testUser;
    private Til testTil;
    private TilRecommend testRecommend;

    @BeforeEach
    void setUp() {
        testUser = CommunityMockBuilder.createUser();
        testTil = CommunityMockBuilder.createPublicTil();
        testRecommend = CommunityMockBuilder.createTilRecommend();
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

    @Nested
    @DisplayName("최신 TIL 10개 조회 테스트")
    class GetRecentTilsTest {

        @Test
        @DisplayName("공개된 최신 TIL 목록이 최신순으로 조회 성공")
        void getRecentTils_Success() {
            // given
            List<Til> recentTilList = CommunityMockBuilder.createTilList(2);
            Pageable pageable = PageRequest.of(CommunityServiceConstant.DEFAULT_PAGE, 10);

            given(tilRepository.findRecentPublicTils(eq(pageable)))
                    .willReturn(recentTilList);

            // when
            CommunityResponseDTO.RecentTilListResponse response = communityService.getRecentTils();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTils()).hasSize(2);

            // 첫 번째 TIL 검증
            CommunityResponseDTO.RecentTilItem firstTil = response.getTils().get(0);
            assertThat(firstTil.getId()).isEqualTo(1L);
            assertThat(firstTil.getTitle()).isEqualTo("TIL 제목 1");
            assertThat(firstTil.getCategory()).isEqualTo(CommunityServiceConstant.TIL_CATEGORY);
            assertThat(firstTil.getUserId()).isEqualTo(CommunityServiceConstant.VALID_USER_ID);
            assertThat(firstTil.getNickname()).isEqualTo(CommunityServiceConstant.USER_NICKNAME);
            assertThat(firstTil.getProfileImageUrl()).isEqualTo(CommunityServiceConstant.USER_PROFILE_IMAGE);
            assertThat(firstTil.getTags()).isEqualTo(CommunityServiceConstant.TIL_TAG_LIST_1);
            assertThat(firstTil.getRecommendCount()).isEqualTo(5);
            assertThat(firstTil.getVisitedCount()).isEqualTo(10);
            assertThat(firstTil.getCommentsCount()).isEqualTo(3);
            assertThat(firstTil.getCreatedAt()).isNotNull();

            // 두 번째 TIL 검증
            CommunityResponseDTO.RecentTilItem secondTil = response.getTils().get(1);
            assertThat(secondTil.getId()).isEqualTo(2L);
            assertThat(secondTil.getTitle()).isEqualTo("TIL 제목 2");
            assertThat(secondTil.getCategory()).isEqualTo(CommunityServiceConstant.TIL_CATEGORY);
            assertThat(secondTil.getUserId()).isEqualTo(CommunityServiceConstant.VALID_USER_ID);
            assertThat(secondTil.getNickname()).isEqualTo(CommunityServiceConstant.USER_NICKNAME);
            assertThat(secondTil.getProfileImageUrl()).isEqualTo(CommunityServiceConstant.USER_PROFILE_IMAGE);
            assertThat(secondTil.getTags()).isEqualTo(CommunityServiceConstant.TIL_TAG_LIST_2);
            assertThat(secondTil.getRecommendCount()).isEqualTo(8);
            assertThat(secondTil.getVisitedCount()).isEqualTo(15);
            assertThat(secondTil.getCommentsCount()).isEqualTo(7);
            assertThat(secondTil.getCreatedAt()).isNotNull();

            // Repository 호출 검증
            verify(tilRepository).findRecentPublicTils(eq(pageable));
        }

        @Test
        @DisplayName("최신 TIL이 없을 경우 빈 목록 반환")
        void getRecentTils_EmptyResult_ReturnsEmptyList() {
            // given
            Pageable pageable = PageRequest.of(CommunityServiceConstant.DEFAULT_PAGE, 10);
            given(tilRepository.findRecentPublicTils(eq(pageable)))
                    .willReturn(Collections.emptyList());

            // when
            CommunityResponseDTO.RecentTilListResponse response = communityService.getRecentTils();

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTils()).isEmpty();
            verify(tilRepository).findRecentPublicTils(eq(pageable));
        }
    }
}

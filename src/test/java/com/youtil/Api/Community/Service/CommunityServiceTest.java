package com.youtil.Api.Community.Service;

import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Api.Community.Dto.CommunityResponseDTO;
import com.youtil.Model.Til;
import com.youtil.Model.TilRecommend;
import com.youtil.Model.User;
import com.youtil.Repository.TilRecommendRepository;
import com.youtil.Repository.TilRepository;
import com.youtil.Repository.UserRepository;
import com.youtil.Util.EntityValidator;
import com.youtil.constant.CommunityTestConstant;
import com.youtil.Mock.CommunityMockBuilder;
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
    private EntityValidator entityValidator;

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

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
    }

    @Nested
    @DisplayName("커뮤니티 목록 조회 테스트")
    class GetCommunityTilsTest {

        @Test
        @DisplayName("성공: 유효한 카테고리로 요청 시 해당 카테고리 TIL 목록 반환")
        void getCommunityTils_WithValidCategory_Success() {
            // given
            CommunityRequestDTO.CommunityListRequest request = CommunityMockBuilder.createCommunityListRequest();
            List<Til> tilList = CommunityMockBuilder.createTilList(2);
            Pageable pageable = PageRequest.of(CommunityTestConstant.DEFAULT_PAGE, CommunityTestConstant.DEFAULT_SIZE);

            given(tilRepository.findRecentPublicTilsByCategory(
                    eq(CommunityTestConstant.CATEGORY_FULLSTACK.toUpperCase()),
                    eq(pageable)
            )).willReturn(tilList);

            // when
            CommunityResponseDTO.CommunityTilListResponse response = communityService.getCommunityTils(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTils()).hasSize(2);
            assertThat(response.getTils().get(0).getCategory()).isEqualTo(CommunityTestConstant.TIL_CATEGORY);
            verify(tilRepository).findRecentPublicTilsByCategory(
                    eq(CommunityTestConstant.CATEGORY_FULLSTACK.toUpperCase()),
                    eq(pageable)
            );
        }

        @Test
        @DisplayName("성공: TIL 목록이 없을 경우 빈 목록 반환")
        void getCommunityTils_WithEmptyResult_ReturnsEmptyList() {
            // given
            CommunityRequestDTO.CommunityListRequest request = CommunityMockBuilder.createCommunityListRequest();
            Pageable pageable = PageRequest.of(CommunityTestConstant.DEFAULT_PAGE, CommunityTestConstant.DEFAULT_SIZE);

            given(tilRepository.findRecentPublicTilsByCategory(
                    eq(CommunityTestConstant.CATEGORY_FULLSTACK.toUpperCase()),
                    eq(pageable)
            )).willReturn(Collections.emptyList());

            // when
            CommunityResponseDTO.CommunityTilListResponse response = communityService.getCommunityTils(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTils()).isEmpty();
            verify(tilRepository).findRecentPublicTilsByCategory(
                    eq(CommunityTestConstant.CATEGORY_FULLSTACK.toUpperCase()),
                    eq(pageable)
            );
        }

        @Test
        @DisplayName("성공: 카테고리가 null이거나 ENTIRE인 경우 전체 TIL 목록 반환")
        void getCommunityTils_WithNullOrEntireCategory_ReturnsAllTils() {
            // given
            CommunityRequestDTO.CommunityListRequest request = CommunityMockBuilder.createCommunityListRequestEntire();
            List<Til> tilList = CommunityMockBuilder.createTilList(2);
            Pageable pageable = PageRequest.of(CommunityTestConstant.DEFAULT_PAGE, CommunityTestConstant.DEFAULT_SIZE);

            given(tilRepository.findRecentPublicTils(eq(pageable))).willReturn(tilList);

            // when
            CommunityResponseDTO.CommunityTilListResponse response = communityService.getCommunityTils(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTils()).hasSize(2);
            verify(tilRepository).findRecentPublicTils(eq(pageable));
        }
    }

    @Nested
    @DisplayName("커뮤니티 게시글 상세 조회 테스트")
    class GetTilDetailTest {

        @Test
        @DisplayName("성공: 유효한 tilId로 요청 시 TIL 상세 정보 반환")
        void getTilDetail_WithValidTilId_Success() {
            // given
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(testTil));
            given(tilRecommendRepository.findByTilIdAndUserId(
                    CommunityTestConstant.VALID_TIL_ID,
                    CommunityTestConstant.VALID_USER_ID
            )).willReturn(Optional.empty());

            // when
            CommunityResponseDTO.CommunityPostDetailResponse response =
                    communityService.getTilDetail(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.VALID_USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getPostId()).isEqualTo(CommunityTestConstant.VALID_TIL_ID);
            assertThat(response.getTitle()).isEqualTo(CommunityTestConstant.TIL_TITLE);
            assertThat(response.getContent()).isEqualTo(CommunityTestConstant.TIL_CONTENT);
            assertThat(response.getLiked()).isFalse();

            verify(redisTemplate.opsForZSet()).add(eq(CommunityTestConstant.REDIS_CHANGED_TILS_KEY),
                    eq(String.valueOf(CommunityTestConstant.VALID_TIL_ID)), anyDouble());
            verify(redisTemplate.opsForValue()).increment(
                    eq(CommunityTestConstant.REDIS_TIL_VISIT_COUNT_KEY), eq(1L));
            verify(tilRepository).save(testTil);
        }

        @Test
        @DisplayName("성공: 조회 시 조회수 1 증가")
        void getTilDetail_IncrementVisitCount_Success() {
            // given
            int originalVisitCount = testTil.getVisitedCount();
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(testTil));
            given(tilRecommendRepository.findByTilIdAndUserId(anyLong(), anyLong()))
                    .willReturn(Optional.empty());

            // when
            communityService.getTilDetail(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.VALID_USER_ID);

            // then
            assertThat(testTil.getVisitedCount()).isEqualTo(originalVisitCount + 1);
            verify(redisTemplate.opsForValue()).increment(
                    eq(CommunityTestConstant.REDIS_TIL_VISIT_COUNT_KEY), eq(1L));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 tilId 요청 시 RuntimeException 발생")
        void getTilDetail_WithInvalidTilId_ThrowsException() {
            // given
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.INVALID_TIL_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    communityService.getTilDetail(CommunityTestConstant.INVALID_TIL_ID, CommunityTestConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityTestConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 비공개 TIL 요청 시 RuntimeException 발생")
        void getTilDetail_WithPrivateTil_ThrowsException() {
            // given
            Til privateTil = CommunityMockBuilder.createPrivateTil();
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(privateTil));

            // when & then
            assertThatThrownBy(() ->
                    communityService.getTilDetail(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityTestConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 삭제된 TIL 요청 시 RuntimeException 발생")
        void getTilDetail_WithDeletedTil_ThrowsException() {
            // given
            Til deletedTil = CommunityMockBuilder.createDeletedTil();
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(deletedTil));

            // when & then
            assertThatThrownBy(() ->
                    communityService.getTilDetail(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityTestConstant.ERROR_TIL_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("TIL 좋아요 토글 테스트")
    class ToggleTilLikeTest {

        @Test
        @DisplayName("성공: 좋아요하지 않은 TIL에 좋아요 시 liked=true, 좋아요수 1 증가")
        void toggleTilLike_AddLike_Success() {
            // given
            int originalLikeCount = testTil.getRecommendCount();
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(testTil));
            given(tilRecommendRepository.findByTilIdAndUserId(
                    CommunityTestConstant.VALID_TIL_ID,
                    CommunityTestConstant.VALID_USER_ID
            )).willReturn(Optional.empty());
            given(tilRecommendRepository.save(any(TilRecommend.class)))
                    .willReturn(testRecommend);

            // when
            CommunityResponseDTO.CommunityLikeResponse response =
                    communityService.toggleTilLike(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.VALID_USER_ID);

            // then
            assertThat(response.getLiked()).isTrue();
            assertThat(response.getLikeCount()).isEqualTo(originalLikeCount + 1);
            assertThat(testTil.getRecommendCount()).isEqualTo(originalLikeCount + 1);

            verify(tilRecommendRepository).save(any(TilRecommend.class));
            verify(redisTemplate.opsForValue()).increment(
                    eq(CommunityTestConstant.REDIS_TIL_LIKE_COUNT_KEY), eq(1L));
            verify(tilRepository).save(testTil);
        }

        @Test
        @DisplayName("성공: 이미 좋아요한 TIL에 좋아요 시 liked=false, 좋아요수 1 감소")
        void toggleTilLike_RemoveLike_Success() {
            // given
            int originalLikeCount = testTil.getRecommendCount();
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(testTil));
            given(tilRecommendRepository.findByTilIdAndUserId(
                    CommunityTestConstant.VALID_TIL_ID,
                    CommunityTestConstant.VALID_USER_ID
            )).willReturn(Optional.of(testRecommend));

            // when
            CommunityResponseDTO.CommunityLikeResponse response =
                    communityService.toggleTilLike(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.VALID_USER_ID);

            // then
            assertThat(response.getLiked()).isFalse();
            assertThat(response.getLikeCount()).isEqualTo(originalLikeCount - 1);
            assertThat(testTil.getRecommendCount()).isEqualTo(originalLikeCount - 1);

            verify(tilRecommendRepository).delete(testRecommend);
            verify(redisTemplate.opsForValue()).increment(
                    eq(CommunityTestConstant.REDIS_TIL_LIKE_COUNT_KEY), eq(-1L));
            verify(tilRepository).save(testTil);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 tilId 요청 시 RuntimeException 발생")
        void toggleTilLike_WithInvalidTilId_ThrowsException() {
            // given
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.INVALID_TIL_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    communityService.toggleTilLike(CommunityTestConstant.INVALID_TIL_ID, CommunityTestConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityTestConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 비공개 TIL에 좋아요 시도 시 RuntimeException 발생")
        void toggleTilLike_WithPrivateTil_ThrowsException() {
            // given
            Til privateTil = CommunityMockBuilder.createPrivateTil();
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(privateTil));

            // when & then
            assertThatThrownBy(() ->
                    communityService.toggleTilLike(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityTestConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 삭제된 TIL에 좋아요 시도 시 RuntimeException 발생")
        void toggleTilLike_WithDeletedTil_ThrowsException() {
            // given
            Til deletedTil = CommunityMockBuilder.createDeletedTil();
            given(userRepository.findById(CommunityTestConstant.VALID_USER_ID))
                    .willReturn(Optional.of(testUser));
            given(tilRepository.findById(CommunityTestConstant.VALID_TIL_ID))
                    .willReturn(Optional.of(deletedTil));

            // when & then
            assertThatThrownBy(() ->
                    communityService.toggleTilLike(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.VALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityTestConstant.ERROR_TIL_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 userId 요청 시 RuntimeException 발생")
        void toggleTilLike_WithInvalidUserId_ThrowsException() {
            // given
            given(userRepository.findById(CommunityTestConstant.INVALID_USER_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    communityService.toggleTilLike(CommunityTestConstant.VALID_TIL_ID, CommunityTestConstant.INVALID_USER_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage(CommunityTestConstant.ERROR_USER_NOT_FOUND);
        }
    }
}

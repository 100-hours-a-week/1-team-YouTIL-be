package com.youtil.Mock;

import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Til;
import com.youtil.Model.TilRecommend;
import com.youtil.Model.User;
import com.youtil.constant.CommunityTestConstant;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 커뮤니티 테스트용 Mock 객체 생성 클래스
 */
public class CommunityMockBuilder {

    /**
     * 테스트용 User 객체 생성
     */
    public static User createUser() {
        return User.builder()
                .id(CommunityTestConstant.VALID_USER_ID)
                .email(CommunityTestConstant.USER_EMAIL)
                .nickname(CommunityTestConstant.USER_NICKNAME)
                .profileImageUrl(CommunityTestConstant.USER_PROFILE_IMAGE)
                .status(Status.active)
                .build();
    }

    /**
     * 테스트용 User 객체 생성 (커스텀 ID)
     */
    public static User createUser(Long userId) {
        return User.builder()
                .id(userId)
                .email(CommunityTestConstant.USER_EMAIL)
                .nickname(CommunityTestConstant.USER_NICKNAME)
                .profileImageUrl(CommunityTestConstant.USER_PROFILE_IMAGE)
                .status(Status.active)
                .build();
    }

    /**
     * 테스트용 공개 TIL 객체 생성
     */
    public static Til createPublicTil() {
        User user = createUser();
        return Til.builder()
                .id(CommunityTestConstant.VALID_TIL_ID)
                .user(user)
                .title(CommunityTestConstant.TIL_TITLE)
                .content(CommunityTestConstant.TIL_CONTENT)
                .isDisplay(CommunityTestConstant.TIL_IS_DISPLAY_TRUE)
                .category(CommunityTestConstant.TIL_CATEGORY)
                .tag(CommunityTestConstant.TIL_TAG_LIST)
                .recommendCount(CommunityTestConstant.TIL_RECOMMEND_COUNT)
                .visitedCount(CommunityTestConstant.TIL_VISITED_COUNT)
                .commentsCount(CommunityTestConstant.TIL_COMMENTS_COUNT)
                .status(CommunityTestConstant.TIL_STATUS_ACTIVE)
                .build();
    }

    /**
     * 테스트용 비공개 TIL 객체 생성
     */
    public static Til createPrivateTil() {
        User user = createUser();
        return Til.builder()
                .id(CommunityTestConstant.VALID_TIL_ID)
                .user(user)
                .title(CommunityTestConstant.TIL_TITLE)
                .content(CommunityTestConstant.TIL_CONTENT)
                .isDisplay(CommunityTestConstant.TIL_IS_DISPLAY_FALSE)
                .category(CommunityTestConstant.TIL_CATEGORY)
                .tag(CommunityTestConstant.TIL_TAG_LIST)
                .recommendCount(CommunityTestConstant.TIL_RECOMMEND_COUNT)
                .visitedCount(CommunityTestConstant.TIL_VISITED_COUNT)
                .commentsCount(CommunityTestConstant.TIL_COMMENTS_COUNT)
                .status(CommunityTestConstant.TIL_STATUS_ACTIVE)
                .build();
    }

    /**
     * 테스트용 삭제된 TIL 객체 생성
     */
    public static Til createDeletedTil() {
        User user = createUser();
        return Til.builder()
                .id(CommunityTestConstant.VALID_TIL_ID)
                .user(user)
                .title(CommunityTestConstant.TIL_TITLE)
                .content(CommunityTestConstant.TIL_CONTENT)
                .isDisplay(CommunityTestConstant.TIL_IS_DISPLAY_TRUE)
                .category(CommunityTestConstant.TIL_CATEGORY)
                .tag(CommunityTestConstant.TIL_TAG_LIST)
                .recommendCount(CommunityTestConstant.TIL_RECOMMEND_COUNT)
                .visitedCount(CommunityTestConstant.TIL_VISITED_COUNT)
                .commentsCount(CommunityTestConstant.TIL_COMMENTS_COUNT)
                .status(CommunityTestConstant.TIL_STATUS_DEACTIVE)
                .build();
    }

    /**
     * 테스트용 TIL 리스트 생성
     */
    public static List<Til> createTilList(int count) {
        User user = createUser();
        return Arrays.asList(
                Til.builder()
                        .id(1L)
                        .user(user)
                        .title("TIL 제목 1")
                        .content("TIL 내용 1")
                        .isDisplay(true)
                        .category(CommunityTestConstant.TIL_CATEGORY)
                        .tag(CommunityTestConstant.TIL_TAG_LIST_1)
                        .recommendCount(5)
                        .visitedCount(10)
                        .commentsCount(3)
                        .status(Status.active)
                        .build(),
                Til.builder()
                        .id(2L)
                        .user(user)
                        .title("TIL 제목 2")
                        .content("TIL 내용 2")
                        .isDisplay(true)
                        .category(CommunityTestConstant.TIL_CATEGORY)
                        .tag(CommunityTestConstant.TIL_TAG_LIST_2)
                        .recommendCount(8)
                        .visitedCount(15)
                        .commentsCount(7)
                        .status(Status.active)
                        .build()
        );
    }

    /**
     * 테스트용 TilRecommend 객체 생성
     */
    public static TilRecommend createTilRecommend() {
        User user = createUser();
        Til til = createPublicTil();
        return TilRecommend.builder()
                .id(CommunityTestConstant.VALID_RECOMMEND_ID)
                .til(til)
                .user(user)
                .build();
    }

    /**
     * 테스트용 CommunityListRequest 생성
     */
    public static CommunityRequestDTO.CommunityListRequest createCommunityListRequest() {
        return CommunityRequestDTO.CommunityListRequest.builder()
                .category(CommunityTestConstant.CATEGORY_FULLSTACK)
                .page(CommunityTestConstant.DEFAULT_PAGE)
                .offset(CommunityTestConstant.DEFAULT_SIZE)
                .build();
    }

    /**
     * 테스트용 CommunityListRequest 생성 (카테고리 없음)
     */
    public static CommunityRequestDTO.CommunityListRequest createCommunityListRequestWithoutCategory() {
        return CommunityRequestDTO.CommunityListRequest.builder()
                .category(null)
                .page(CommunityTestConstant.DEFAULT_PAGE)
                .offset(CommunityTestConstant.DEFAULT_SIZE)
                .build();
    }

    /**
     * 테스트용 CommunityListRequest 생성 (전체 카테고리)
     */
    public static CommunityRequestDTO.CommunityListRequest createCommunityListRequestEntire() {
        return CommunityRequestDTO.CommunityListRequest.builder()
                .category(CommunityTestConstant.CATEGORY_ENTIRE)
                .page(CommunityTestConstant.DEFAULT_PAGE)
                .offset(CommunityTestConstant.DEFAULT_SIZE)
                .build();
    }
}

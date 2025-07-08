package com.youtil.Mock;

import com.youtil.Api.Community.Dto.CommunityRequestDTO;
import com.youtil.Constants.CommunityServiceConstant;
import com.youtil.Common.Enums.Status;
import com.youtil.Model.Til;
import com.youtil.Model.TilRecommend;
import com.youtil.Model.User;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public class CommunityMockBuilder {

    /**
     * 테스트용 User 객체 생성
     */
    public static User createUser() {
        User user = User.builder()
                .id(CommunityServiceConstant.VALID_USER_ID)
                .email(CommunityServiceConstant.USER_EMAIL)
                .nickname(CommunityServiceConstant.USER_NICKNAME)
                .profileImageUrl(CommunityServiceConstant.USER_PROFILE_IMAGE)
                .status(Status.active)
                .build();

        // BaseTime 필드 설정
        setBaseTimeFields(user);
        return user;
    }

    /**
     * 테스트용 User 객체 생성 (커스텀 ID)
     */
    public static User createUser(Long userId) {
        User user = User.builder()
                .id(userId)
                .email(CommunityServiceConstant.USER_EMAIL)
                .nickname(CommunityServiceConstant.USER_NICKNAME)
                .profileImageUrl(CommunityServiceConstant.USER_PROFILE_IMAGE)
                .status(Status.active)
                .build();

        // BaseTime 필드 설정
        setBaseTimeFields(user);
        return user;
    }

    /**
     * 테스트용 공개 TIL 객체 생성
     */
    public static Til createPublicTil() {
        User user = createUser();
        Til til = Til.builder()
                .id(CommunityServiceConstant.VALID_TIL_ID)
                .user(user)
                .title(CommunityServiceConstant.TIL_TITLE)
                .content(CommunityServiceConstant.TIL_CONTENT)
                .isDisplay(CommunityServiceConstant.TIL_IS_DISPLAY_TRUE)
                .category(CommunityServiceConstant.TIL_CATEGORY)
                .tag(CommunityServiceConstant.TIL_TAG_LIST)
                .recommendCount(CommunityServiceConstant.TIL_RECOMMEND_COUNT)
                .visitedCount(CommunityServiceConstant.TIL_VISITED_COUNT)
                .commentsCount(CommunityServiceConstant.TIL_COMMENTS_COUNT)
                .status(CommunityServiceConstant.TIL_STATUS_ACTIVE)
                .build();

        // BaseTime 필드 설정
        setBaseTimeFields(til);
        return til;
    }

    /**
     * 테스트용 비공개 TIL 객체 생성
     */
    public static Til createPrivateTil() {
        User user = createUser();
        Til til = Til.builder()
                .id(CommunityServiceConstant.VALID_TIL_ID)
                .user(user)
                .title(CommunityServiceConstant.TIL_TITLE)
                .content(CommunityServiceConstant.TIL_CONTENT)
                .isDisplay(CommunityServiceConstant.TIL_IS_DISPLAY_FALSE)
                .category(CommunityServiceConstant.TIL_CATEGORY)
                .tag(CommunityServiceConstant.TIL_TAG_LIST)
                .recommendCount(CommunityServiceConstant.TIL_RECOMMEND_COUNT)
                .visitedCount(CommunityServiceConstant.TIL_VISITED_COUNT)
                .commentsCount(CommunityServiceConstant.TIL_COMMENTS_COUNT)
                .status(CommunityServiceConstant.TIL_STATUS_ACTIVE)
                .build();

        // BaseTime 필드 설정
        setBaseTimeFields(til);
        return til;
    }

    /**
     * 테스트용 삭제된 TIL 객체 생성
     */
    public static Til createDeletedTil() {
        User user = createUser();
        Til til = Til.builder()
                .id(CommunityServiceConstant.VALID_TIL_ID)
                .user(user)
                .title(CommunityServiceConstant.TIL_TITLE)
                .content(CommunityServiceConstant.TIL_CONTENT)
                .isDisplay(CommunityServiceConstant.TIL_IS_DISPLAY_TRUE)
                .category(CommunityServiceConstant.TIL_CATEGORY)
                .tag(CommunityServiceConstant.TIL_TAG_LIST)
                .recommendCount(CommunityServiceConstant.TIL_RECOMMEND_COUNT)
                .visitedCount(CommunityServiceConstant.TIL_VISITED_COUNT)
                .commentsCount(CommunityServiceConstant.TIL_COMMENTS_COUNT)
                .status(CommunityServiceConstant.TIL_STATUS_DEACTIVE)
                .build();

        // BaseTime 필드 설정
        setBaseTimeFields(til);
        return til;
    }

    /**
     * 테스트용 TIL 리스트 생성
     */
    public static List<Til> createTilList(int count) {
        User user = createUser();

        Til til1 = Til.builder()
                .id(1L)
                .user(user)
                .title("TIL 제목 1")
                .content("TIL 내용 1")
                .isDisplay(true)
                .category(CommunityServiceConstant.TIL_CATEGORY)
                .tag(CommunityServiceConstant.TIL_TAG_LIST_1)
                .recommendCount(5)
                .visitedCount(10)
                .commentsCount(3)
                .status(Status.active)
                .build();

        Til til2 = Til.builder()
                .id(2L)
                .user(user)
                .title("TIL 제목 2")
                .content("TIL 내용 2")
                .isDisplay(true)
                .category(CommunityServiceConstant.TIL_CATEGORY)
                .tag(CommunityServiceConstant.TIL_TAG_LIST_2)
                .recommendCount(8)
                .visitedCount(15)
                .commentsCount(7)
                .status(Status.active)
                .build();

        // BaseTime 필드 설정
        setBaseTimeFields(til1);
        setBaseTimeFields(til2);

        return Arrays.asList(til1, til2);
    }

    /**
     * 테스트용 TilRecommend 객체 생성
     */
    public static TilRecommend createTilRecommend() {
        User user = createUser();
        Til til = createPublicTil();
        TilRecommend recommend = TilRecommend.builder()
                .id(CommunityServiceConstant.VALID_RECOMMEND_ID)
                .til(til)
                .user(user)
                .build();

        setCreatedAtField(recommend);
        return recommend;
    }

    /**
     * 테스트용 CommunityListRequest 생성
     */
    public static CommunityRequestDTO.CommunityListRequest createCommunityListRequest() {
        return CommunityRequestDTO.CommunityListRequest.builder()
                .category(CommunityServiceConstant.CATEGORY_FULLSTACK)
                .page(CommunityServiceConstant.DEFAULT_PAGE)
                .offset(CommunityServiceConstant.DEFAULT_SIZE)
                .build();
    }

    /**
     * BaseTime을 상속받는 객체의 createdAt, updatedAt 필드 설정
     */
    private static void setBaseTimeFields(Object entity) {
        try {
            OffsetDateTime now = OffsetDateTime.now();

            // createdAt 필드 설정 (BaseTime 클래스에서 찾기)
            java.lang.reflect.Field createdAtField = findFieldInHierarchy(entity.getClass(), "createdAt");
            if (createdAtField != null) {
                createdAtField.setAccessible(true);
                createdAtField.set(entity, now);
            }

            // updatedAt 필드 설정 (BaseTime 클래스에서 찾기)
            java.lang.reflect.Field updatedAtField = findFieldInHierarchy(entity.getClass(), "updatedAt");
            if (updatedAtField != null) {
                updatedAtField.setAccessible(true);
                updatedAtField.set(entity, now);
            }
        } catch (Exception e) {
            // 필드 설정에 실패해도 테스트에 영향주지 않도록 로그만 남김
            System.err.println("BaseTime 필드 설정 실패: " + e.getMessage());
        }
    }

    /**
     * TilRecommend의 createdAt 필드 설정
     */
    private static void setCreatedAtField(TilRecommend recommend) {
        try {
            java.lang.reflect.Field createdAtField = TilRecommend.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(recommend, OffsetDateTime.now());
        } catch (Exception e) {
            System.err.println("TilRecommend createdAt 필드 설정 실패: " + e.getMessage());
        }
    }

    /**
     * 클래스 상속 구조에서 특정 필드를 찾는 헬퍼 메서드
     */
    private static java.lang.reflect.Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}

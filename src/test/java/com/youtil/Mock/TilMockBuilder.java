package com.youtil.Mock;

import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Dto.TilUploadRequestDTO;
import com.youtil.Api.Tils.Dto.TilUploadResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Constants.TilTestConstants;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * TIL 테스트용 Mock 객체 생성 빌더
 */
public final class TilMockBuilder {

    // ======================== User 엔티티 생성 ========================

    public static User createTestUser() {
        return createTestUser(TilTestConstants.TEST_USER_ID);
    }

    public static Til createMockTil(User user) {
        return Til.builder()
                .id(TilTestConstants.MOCK_TIL_ID)
                .user(user)
                .status(Status.active)
                .title(TilTestConstants.MOCK_TITLE)
                .content(TilTestConstants.MOCK_CONTENT)
                .tag(TilTestConstants.MOCK_TAGS)
                .category(TilTestConstants.MOCK_CATEGORY)
                .commentsCount(TilTestConstants.INITIAL_COMMENTS_COUNT)
                .visitedCount(TilTestConstants.INITIAL_VISITED_COUNT)
                .isDisplay(TilTestConstants.IS_DISPLAYED)
                .recommendCount(TilTestConstants.INITIAL_RECOMMEND_COUNT)
                .build();
    }


    public static User createTestUser(Long userId) {
        return User.builder()
                .id(userId)
                .email(TilTestConstants.TEST_USER_EMAIL)
                .nickname(TilTestConstants.TEST_USER_NICKNAME)
                .profileImageUrl(TilTestConstants.TEST_USER_PROFILE_IMAGE)
                .description(TilTestConstants.TEST_USER_DESCRIPTION)
                .githubToken(TilTestConstants.GITHUB_TOKEN)
                .uploadRepository(TilTestConstants.GITHUB_REPOSITORY_CONFIG)
                .status(Status.active)
                .build();
    }

    /**
     * 테스트용 Til 엔티티 생성
     */
    public static Til createTestTil() {
        return createTestTil(createTestUser());
    }

    public static Til createTestTil(User user) {
        return createTestTil(TilTestConstants.TEST_TIL_ID, user);
    }

    public static Til createTestTil(Long tilId, User user) {
        Til til = Til.builder()
                .id(tilId)
                .user(user)
                .title(TilTestConstants.TEST_TIL_TITLE)
                .content(TilTestConstants.TEST_TIL_CONTENT)
                .category(TilTestConstants.TEST_TIL_CATEGORY)
                .tag(TilTestConstants.TEST_TIL_TAGS)
                .isDisplay(true)
                .commitRepository(TilTestConstants.TEST_TIL_REPOSITORY)
                .isUploaded(false)
                .recommendCount(TilTestConstants.DEFAULT_RECOMMEND_COUNT)
                .visitedCount(TilTestConstants.DEFAULT_VISITED_COUNT)
                .commentsCount(TilTestConstants.DEFAULT_COMMENTS_COUNT)
                .status(Status.active)
                .build();

        til.setCreatedAt(TilTestConstants.TEST_OFFSET_DATETIME);
        til.setUpdatedAt(TilTestConstants.TEST_OFFSET_DATETIME);
        return til;
    }

    // ======================== TIL Request DTO 생성 ========================

    public static TilRequestDTO.CreateWithAiRequest createValidCreateWithAiRequest() {
        return TilRequestDTO.CreateWithAiRequest.builder()
                .repositoryId(TilTestConstants.GITHUB_REPOSITORY_ID)
                .branch(TilTestConstants.GITHUB_BRANCH)
                .commits(Arrays.asList(createCommitSummary()))
                .title(TilTestConstants.TEST_TIL_TITLE)
                .category(TilTestConstants.TEST_TIL_CATEGORY)
                .isShared(true)
                .build();
    }

    public static TilRequestDTO.CreateWithAiRequest createInvalidCreateWithAiRequest() {
        return TilRequestDTO.CreateWithAiRequest.builder()
                .repositoryId(null) // 필수 필드 누락
                .branch(TilTestConstants.GITHUB_BRANCH)
                .commits(Arrays.asList(createCommitSummary()))
                .title(TilTestConstants.TEST_TIL_TITLE)
                .category(TilTestConstants.TEST_TIL_CATEGORY)
                .isShared(true)
                .build();
    }

    public static TilRequestDTO.CommitSummary createCommitSummary() {
        return TilRequestDTO.CommitSummary.builder()
                .sha(TilTestConstants.COMMIT_SHA)
                .message(TilTestConstants.COMMIT_MESSAGE)
                .build();
    }

    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequest() {
        return createUpdateTilRequest(TilTestConstants.TEST_TIL_ID, TilTestConstants.UPDATED_TIL_TITLE);
    }

    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequest(Long tilId, String title) {
        return TilRequestDTO.UpdateTilRequest.builder()
                .tilId(tilId)
                .title(title)
                .build();
    }

    public static TilRequestDTO.BatchDeleteTilRequest createBatchDeleteTilRequest() {
        return TilRequestDTO.BatchDeleteTilRequest.builder()
                .tilIds(Arrays.asList(TilTestConstants.TEST_TIL_ID))
                .build();
    }

    // ======================== TIL Response DTO 생성 ========================

    public static TilResponseDTO.CreateTilResponse createSuccessCreateTilResponse() {
        return TilResponseDTO.CreateTilResponse.builder()
                .tilID(TilTestConstants.TEST_TIL_ID)
                .build();
    }

    public static TilResponseDTO.CreateTilResponse createFailedCreateTilResponse() {
        return TilResponseDTO.CreateTilResponse.builder()
                .tilID(null) // 실패 시 null
                .build();
    }

    public static TilResponseDTO.TilDetailResponse createTilDetailResponse() {
        return createTilDetailResponse(createTestTil());
    }

    public static TilResponseDTO.TilDetailResponse createTilDetailResponse(Til til) {
        return TilResponseDTO.TilDetailResponse.builder()
                .id(til.getId())
                .userId(til.getUser().getId())
                .nickname(til.getUser().getNickname())
                .profileImageUrl(til.getUser().getProfileImageUrl())
                .title(til.getTitle())
                .content(til.getContent())
                .category(til.getCategory())
                .tag(til.getTag())
                .isDisplay(til.getIsDisplay())
                .commitRepository(til.getCommitRepository())
                .isUploaded(til.getIsUploaded())
                .recommendCount(til.getRecommendCount())
                .visitedCount(til.getVisitedCount())
                .commentsCount(til.getCommentsCount())
                .createdAt(til.getCreatedAt())
                .updatedAt(til.getUpdatedAt())
                .build();
    }

    public static TilResponseDTO.TilListResponse createTilListResponse() {
        return TilResponseDTO.TilListResponse.builder()
                .tils(createTilListItems())
                .build();
    }

    public static TilResponseDTO.TilListResponse createEmptyTilListResponse() {
        return TilResponseDTO.TilListResponse.builder()
                .tils(Collections.emptyList())
                .build();
    }

    public static List<UserResponseDTO.TilListItem> createTilListItems() {
        UserResponseDTO.TilListItem item1 = new UserResponseDTO.TilListItem(
                TilTestConstants.TEST_USER_ID,
                TilTestConstants.TEST_USER_NICKNAME,
                TilTestConstants.TEST_USER_PROFILE_IMAGE,
                TilTestConstants.TEST_TIL_ID,
                TilTestConstants.TEST_TIL_TITLE,
                TilTestConstants.TEST_TIL_TAGS,
                TilTestConstants.TEST_OFFSET_DATETIME,
                TilTestConstants.DEFAULT_VISITED_COUNT,
                TilTestConstants.DEFAULT_RECOMMEND_COUNT,
                TilTestConstants.DEFAULT_COMMENTS_COUNT
        );

        UserResponseDTO.TilListItem item2 = new UserResponseDTO.TilListItem(
                TilTestConstants.TEST_USER_ID,
                TilTestConstants.TEST_USER_NICKNAME,
                TilTestConstants.TEST_USER_PROFILE_IMAGE,
                TilTestConstants.ANOTHER_TIL_ID,
                "두 번째 TIL",
                Arrays.asList("React", "JavaScript"),
                TilTestConstants.TEST_OFFSET_DATETIME,
                15, 8, 3
        );

        return Arrays.asList(item1, item2);
    }

    // ======================== Upload 관련 DTO 생성 ========================

    public static TilUploadRequestDTO.UploadRequest createTilUploadRequest() {
        return createTilUploadRequest(TilTestConstants.TEST_TIL_ID);
    }

    public static TilUploadRequestDTO.UploadRequest createTilUploadRequest(Long tilId) {
        return TilUploadRequestDTO.UploadRequest.builder()
                .tilId(tilId)
                .build();
    }

    public static TilUploadResponseDTO.UploadToGitHubResponse createSuccessUploadResponse() {
        return TilUploadResponseDTO.UploadToGitHubResponse.builder()
                .success(true)
                .fileUrl(TilTestConstants.GITHUB_FILE_URL)
                .commitSha(TilTestConstants.GITHUB_COMMIT_SHA)
                .uploadedFilePath(TilTestConstants.GITHUB_FILE_PATH)
                .message(TilTestConstants.SUCCESS_TIL_UPLOADED)
                .build();
    }

    // ======================== 특수 케이스 빌더 ========================

    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequestWithLongTitle() {
        return createUpdateTilRequest(TilTestConstants.TEST_TIL_ID, TilTestConstants.LONG_TITLE_41_CHARS);
    }

    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequestWith40CharsTitle() {
        return createUpdateTilRequest(TilTestConstants.TEST_TIL_ID, TilTestConstants.VALID_TITLE_40_CHARS);
    }

    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequestWithNullTilId() {
        return createUpdateTilRequest(null, TilTestConstants.UPDATED_TIL_TITLE);
    }

    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequestWithNullTitle() {
        return createUpdateTilRequest(TilTestConstants.TEST_TIL_ID, null);
    }

    public static TilRequestDTO.BatchDeleteTilRequest createBatchDeleteTilRequestWithNullIds() {
        return TilRequestDTO.BatchDeleteTilRequest.builder()
                .tilIds(null)
                .build();
    }

    public static TilRequestDTO.BatchDeleteTilRequest createBatchDeleteTilRequestWithEmptyIds() {
        return TilRequestDTO.BatchDeleteTilRequest.builder()
                .tilIds(Collections.emptyList())
                .build();
    }

    public static TilRequestDTO.BatchDeleteTilRequest createBatchDeleteTilRequestWithNonExistentId() {
        return TilRequestDTO.BatchDeleteTilRequest.builder()
                .tilIds(Arrays.asList(TilTestConstants.NON_EXISTENT_TIL_ID))
                .build();
    }

    public static TilUploadRequestDTO.UploadRequest createTilUploadRequestWithNullTilId() {
        return createTilUploadRequest(null);
    }

    private TilMockBuilder() {
        // 인스턴스 생성 방지
    }
}

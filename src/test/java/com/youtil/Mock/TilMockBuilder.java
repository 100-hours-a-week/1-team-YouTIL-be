package com.youtil.Mock;

import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Dto.TilUploadRequestDTO;
import com.youtil.Api.Tils.Dto.TilUploadResponseDTO;
import com.youtil.Common.Enums.Status;
import com.youtil.Constants.TilTestConstants;
import com.youtil.Model.Til;
import com.youtil.Model.User;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;

/**
 * TIL 테스트용 Mock 객체 생성 빌더
 */
public final class TilMockBuilder {

    /**
     * 테스트용 User ㄱ9엔티티 생성
     */
    public static User createTestUser() {
        return createTestUser(TilTestConstants.TEST_USER_ID);
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
        return Til.builder()
                .id(TilTestConstants.TEST_TIL_ID)
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
    }

    /**
     * TIL 수정 요청 DTO 생성
     */
    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequest() {
        return createUpdateTilRequest(TilTestConstants.TEST_TIL_ID, TilTestConstants.UPDATED_TIL_TITLE);
    }

    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequest(Long tilId, String title) {
        return TilRequestDTO.UpdateTilRequest.builder()
                .tilId(tilId)
                .title(title)
                .build();
    }

    /**
     * 40자 초과 제목으로 TIL 수정 요청 DTO 생성
     */
    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequestWithLongTitle() {
        return createUpdateTilRequest(TilTestConstants.TEST_TIL_ID, TilTestConstants.LONG_TITLE_41_CHARS);
    }

    /**
     * 정확히 40자 제목으로 TIL 수정 요청 DTO 생성
     */
    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequestWith40CharsTitle() {
        return createUpdateTilRequest(TilTestConstants.TEST_TIL_ID, TilTestConstants.VALID_TITLE_40_CHARS);
    }

    /**
     * null tilId로 TIL 수정 요청 DTO 생성
     */
    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequestWithNullTilId() {
        return createUpdateTilRequest(null, TilTestConstants.UPDATED_TIL_TITLE);
    }

    /**
     * null title로 TIL 수정 요청 DTO 생성
     */
    public static TilRequestDTO.UpdateTilRequest createUpdateTilRequestWithNullTitle() {
        return createUpdateTilRequest(TilTestConstants.TEST_TIL_ID, null);
    }

    /**
     * TIL 일괄 삭제 요청 DTO 생성
     */
    public static TilRequestDTO.BatchDeleteTilRequest createBatchDeleteTilRequest() {
        return TilRequestDTO.BatchDeleteTilRequest.builder()
                .tilIds(Arrays.asList(TilTestConstants.TEST_TIL_ID))
                .build();
    }

    /**
     * null TIL ID 목록으로 일괄 삭제 요청 DTO 생성
     */
    public static TilRequestDTO.BatchDeleteTilRequest createBatchDeleteTilRequestWithNullIds() {
        return TilRequestDTO.BatchDeleteTilRequest.builder()
                .tilIds(null)
                .build();
    }

    /**
     * 빈 TIL ID 목록으로 일괄 삭제 요청 DTO 생성
     */
    public static TilRequestDTO.BatchDeleteTilRequest createBatchDeleteTilRequestWithEmptyIds() {
        return TilRequestDTO.BatchDeleteTilRequest.builder()
                .tilIds(Collections.emptyList())
                .build();
    }

    /**
     * 존재하지 않는 TIL ID로 일괄 삭제 요청 DTO 생성
     */
    public static TilRequestDTO.BatchDeleteTilRequest createBatchDeleteTilRequestWithNonExistentId() {
        return TilRequestDTO.BatchDeleteTilRequest.builder()
                .tilIds(Arrays.asList(TilTestConstants.NON_EXISTENT_TIL_ID))
                .build();
    }

    /**
     * TIL 업로드 요청 DTO 생성
     */
    public static TilUploadRequestDTO.UploadRequest createTilUploadRequest() {
        return createTilUploadRequest(TilTestConstants.TEST_TIL_ID);
    }

    public static TilUploadRequestDTO.UploadRequest createTilUploadRequest(Long tilId) {
        return TilUploadRequestDTO.UploadRequest.builder()
                .tilId(tilId)
                .build();
    }

    /**
     * null tilId로 TIL 업로드 요청 DTO 생성
     */
    public static TilUploadRequestDTO.UploadRequest createTilUploadRequestWithNullTilId() {
        return createTilUploadRequest(null);
    }

    /**
     * TIL 업로드 성공 응답 DTO 생성
     */
    public static TilUploadResponseDTO.UploadToGitHubResponse createSuccessUploadResponse() {
        return TilUploadResponseDTO.UploadToGitHubResponse.builder()
                .success(true)
                .fileUrl(TilTestConstants.GITHUB_FILE_URL)
                .commitSha(TilTestConstants.GITHUB_COMMIT_SHA)
                .uploadedFilePath(TilTestConstants.GITHUB_FILE_PATH)
                .message(TilTestConstants.SUCCESS_TIL_UPLOADED)
                .build();
    }

    /**
     * TIL 상세 응답 DTO 생성
     */
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
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private TilMockBuilder() {
    }
}

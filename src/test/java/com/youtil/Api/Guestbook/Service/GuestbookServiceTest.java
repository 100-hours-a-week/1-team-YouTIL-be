package com.youtil.Api.Guestbook.Service;

import com.youtil.Api.Guestbook.Service.GuestbookService;
import com.youtil.Api.Guestbook.dto.GuestbookRequestDTO;
import com.youtil.Api.Guestbook.dto.GuestbookResponseDTO;
import com.youtil.Common.Enums.GuestbookStatus;
import com.youtil.Exception.GuestbookException.GuestbookException;
import com.youtil.Model.Guestbook;
import com.youtil.Model.User;
import com.youtil.Repository.GuestbookRepository;
import com.youtil.Util.EntityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static com.youtil.Constants.GuestbookTestConstant.*;
import static com.youtil.Mock.GuestbookTestMockBuilder.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuestbookService 테스트")
class GuestbookServiceTest {

    @InjectMocks
    private GuestbookService guestbookService;

    @Mock
    private GuestbookRepository guestbookRepository;

    @Mock
    private EntityValidator entityValidator;

    private User ownerUser;
    private User guestUser;

    @BeforeEach
    void setUp() {
        ownerUser = createOwnerUser();
        guestUser = createGuestUser();
    }

    @Nested
    @DisplayName("방명록 작성 테스트")
    class CreateGuestbookTest {

        @Nested
        @DisplayName("성공 시나리오")
        class SuccessScenarios {

            @Test
            @DisplayName("유효한 userId, content로 방명록 작성 성공")
            void createGuestbook_WithValidData_Success() {
                // given
                GuestbookRequestDTO.CreateGuestbookRequestDTO request = createValidCreateRequest();
                Guestbook savedGuestbook = createActiveGuestbook();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.save(any(Guestbook.class))).willReturn(savedGuestbook);

                // when
                GuestbookResponseDTO.CreateGuestbookResponseDTO response =
                        guestbookService.createGuestbook(VALID_OWNER_ID, VALID_GUEST_ID, request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getGuestbookId()).isEqualTo(VALID_GUESTBOOK_ID);

                verify(entityValidator).getValidUserOrThrow(VALID_OWNER_ID);
                verify(entityValidator).getValidUserOrThrow(VALID_GUEST_ID);
                verify(guestbookRepository).save(any(Guestbook.class));
            }

            @Test
            @DisplayName("topGuestbookId와 함께 요청시 대댓글 작성 성공")
            void createReply_WithTopGuestbookId_Success() {
                // given
                GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                        createReplyRequest(VALID_PARENT_GUESTBOOK_ID);
                Guestbook parentGuestbook = createActiveGuestbookWithId(VALID_PARENT_GUESTBOOK_ID);
                Guestbook savedReply = createReplyGuestbook(VALID_PARENT_GUESTBOOK_ID);

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findByIdIgnoreStatus(VALID_PARENT_GUESTBOOK_ID))
                        .willReturn(createOptionalGuestbookWithId(VALID_PARENT_GUESTBOOK_ID));
                given(guestbookRepository.save(any(Guestbook.class))).willReturn(savedReply);

                // when
                GuestbookResponseDTO.CreateGuestbookResponseDTO response =
                        guestbookService.createGuestbook(VALID_OWNER_ID, VALID_GUEST_ID, request);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getGuestbookId()).isEqualTo(savedReply.getId());

                verify(guestbookRepository).findByIdIgnoreStatus(VALID_PARENT_GUESTBOOK_ID);
                verify(guestbookRepository).save(any(Guestbook.class));
            }
        }

        @Nested
        @DisplayName("실패 시나리오")
        class FailureScenarios {

            @Test
            @DisplayName("content가 null일 경우 HTTP 400 Bad Request")
            void createGuestbook_WithNullContent_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                        createRequestWithContent(NULL_CONTENT);

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.createGuestbook(VALID_OWNER_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(RuntimeException.class);
            }

            @Test
            @DisplayName("content가 공백일 경우 HTTP 400 Bad Request")
            void createGuestbook_WithEmptyContent_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                        createRequestWithContent(EMPTY_CONTENT);

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.createGuestbook(VALID_OWNER_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(RuntimeException.class);
            }

            @Test
            @DisplayName("content 50자 초과할 경우 HTTP 400 Bad Request")
            void createGuestbook_WithTooLongContent_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                        createRequestWithContent(LONG_CONTENT);

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.createGuestbook(VALID_OWNER_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(RuntimeException.class); // 실제 validation 위치에 따라 조정
            }

            @Test
            @DisplayName("존재하지 않는 userId HTTP 400 Bad Request")
            void createGuestbook_WithInvalidUserId_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.CreateGuestbookRequestDTO request = createValidCreateRequest();

                given(entityValidator.getValidUserOrThrow(INVALID_USER_ID))
                        .willThrow(new RuntimeException(USER_NOT_FOUND_MESSAGE));

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.createGuestbook(INVALID_USER_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage(USER_NOT_FOUND_MESSAGE);
            }

            @Test
            @DisplayName("존재하지 않는 topGuestbookId를 입력 HTTP 404 Not Found")
            void createReply_WithInvalidTopGuestbookId_ThrowsNotFound() {
                // given
                GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                        createReplyRequest(INVALID_GUESTBOOK_ID);

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findByIdIgnoreStatus(INVALID_GUESTBOOK_ID))
                        .willReturn(createEmptyOptional());

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.createGuestbook(VALID_OWNER_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(GuestbookException.InvalidParentGuestbookException.class);
            }

            @Test
            @DisplayName("완전 삭제된 댓글에 대댓글 시도 시 HTTP 400 Bad Request")
            void createReply_ToFullyDeletedGuestbook_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                        createReplyRequest(VALID_PARENT_GUESTBOOK_ID);
                Guestbook softDeletedGuestbook = createSoftDeletedGuestbook();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findByIdIgnoreStatus(VALID_PARENT_GUESTBOOK_ID))
                        .willReturn(Optional.of(softDeletedGuestbook));

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.createGuestbook(VALID_OWNER_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(GuestbookException.CannotReplyToDeletedGuestbookException.class);
            }
        }
    }

    @Nested
    @DisplayName("방명록 리스트 조회 테스트")
    class GetGuestbookListTest {

        @Nested
        @DisplayName("성공 시나리오")
        class SuccessScenarios {

            @Test
            @DisplayName("유효한 userId 요청 시 해당 사용자의 방명록 목록 반환")
            void getGuestbookList_WithValidUserId_ReturnsGuestbookList() {
                // given
                Pageable pageable = createValidPageable();
                var guestbookPage = createMultipleGuestbookPage();
                var replies = createEmptyRepliesList();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(guestbookRepository.findTopLevelGuestbooksByOwnerId(
                        VALID_OWNER_ID, GuestbookStatus.ACTIVE, pageable))
                        .willReturn(guestbookPage);
                given(guestbookRepository.findRepliesByTopGuestbookId(anyLong(), eq(GuestbookStatus.ACTIVE)))
                        .willReturn(replies);

                // when
                GuestbookResponseDTO.GetGuestbookListResponseDTO response =
                        guestbookService.getGuestbookList(VALID_OWNER_ID, pageable);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getGuestbooks()).hasSize(3);
                assertThat(response.getTotalCount()).isEqualTo(3);

                verify(entityValidator).getValidUserOrThrow(VALID_OWNER_ID);
                verify(guestbookRepository).findTopLevelGuestbooksByOwnerId(
                        VALID_OWNER_ID, GuestbookStatus.ACTIVE, pageable);
            }

            @Test
            @DisplayName("삭제된 댓글은 '삭제된 댓글입니다' 메시지로 표시")
            void getGuestbookList_ShowsDeletedCommentMessage() {
                // given
                Pageable pageable = createValidPageable();
                var guestbook = createDeletedGuestbook();
                var guestbookPage = createSingleGuestbookPageWithGuestbook(guestbook);
                var replies = createEmptyRepliesList();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(guestbookRepository.findTopLevelGuestbooksByOwnerId(
                        VALID_OWNER_ID, GuestbookStatus.ACTIVE, pageable))
                        .willReturn(guestbookPage);
                given(guestbookRepository.findRepliesByTopGuestbookId(anyLong(), eq(GuestbookStatus.ACTIVE)))
                        .willReturn(replies);

                // when
                GuestbookResponseDTO.GetGuestbookListResponseDTO response =
                        guestbookService.getGuestbookList(VALID_OWNER_ID, pageable);

                // then
                assertThat(response.getGuestbooks()).hasSize(1);
                assertThat(response.getGuestbooks().get(0).getContent())
                        .isEqualTo(DELETED_COMMENT_MESSAGE);
                assertThat(response.getGuestbooks().get(0).isDeleted()).isTrue();
            }

            @Test
            @DisplayName("방명록이 없는 경우 빈 목록 반환")
            void getGuestbookList_NoGuestbooks_ReturnsEmptyList() {
                // given
                Pageable pageable = createValidPageable();
                var emptyPage = createEmptyGuestbookPage();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(guestbookRepository.findTopLevelGuestbooksByOwnerId(
                        VALID_OWNER_ID, GuestbookStatus.ACTIVE, pageable))
                        .willReturn(emptyPage);

                // when
                GuestbookResponseDTO.GetGuestbookListResponseDTO response =
                        guestbookService.getGuestbookList(VALID_OWNER_ID, pageable);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getGuestbooks()).isEmpty();
                assertThat(response.getTotalCount()).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("실패 시나리오")
        class FailureScenarios {

            @Test
            @DisplayName("존재하지 않는 userId 시 HTTP 400 Bad Request")
            void getGuestbookList_WithInvalidUserId_ThrowsBadRequest() {
                // given
                Pageable pageable = createValidPageable();

                given(entityValidator.getValidUserOrThrow(INVALID_USER_ID))
                        .willThrow(new RuntimeException(USER_NOT_FOUND_MESSAGE));

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.getGuestbookList(INVALID_USER_ID, pageable))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage(USER_NOT_FOUND_MESSAGE);
            }
        }
    }

    @Nested
    @DisplayName("방명록 수정 테스트")
    class UpdateGuestbookTest {

        @Nested
        @DisplayName("성공 시나리오")
        class SuccessScenarios {

            @Test
            @DisplayName("본인이 작성한 방명록 수정 성공")
            void updateGuestbook_OwnGuestbook_Success() {
                // given
                GuestbookRequestDTO.UpdateGuestbookRequestDTO request = createValidUpdateRequest();
                Guestbook guestbook = createActiveGuestbook();
                String originalContent = guestbook.getContent();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findByIdAndGuestIdAndStatus(
                        VALID_GUESTBOOK_ID, VALID_GUEST_ID, GuestbookStatus.ACTIVE))
                        .willReturn(Optional.of(guestbook));

                // when
                guestbookService.updateGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, VALID_GUEST_ID, request);

                // then
                assertThat(guestbook.getContent()).isEqualTo(UpdateScenario.NEW_CONTENT);
                assertThat(guestbook.getContent()).isNotEqualTo(originalContent);

                verify(entityValidator).getValidUserOrThrow(VALID_OWNER_ID);
                verify(entityValidator).getValidUserOrThrow(VALID_GUEST_ID);
                verify(guestbookRepository).findByIdAndGuestIdAndStatus(
                        VALID_GUESTBOOK_ID, VALID_GUEST_ID, GuestbookStatus.ACTIVE);
            }
        }

        @Nested
        @DisplayName("실패 시나리오")
        class FailureScenarios {

            @Test
            @DisplayName("content null일 경우 HTTP 400 Bad Request")
            void updateGuestbook_WithNullContent_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.UpdateGuestbookRequestDTO request =
                        createUpdateRequestWithContent(NULL_CONTENT);

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.updateGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(RuntimeException.class);
            }

            @Test
            @DisplayName("content 공백일 경우 HTTP 400 Bad Request")
            void updateGuestbook_WithEmptyContent_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.UpdateGuestbookRequestDTO request =
                        createUpdateRequestWithContent(EMPTY_CONTENT);

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.updateGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(RuntimeException.class);
            }

            @Test
            @DisplayName("content 50자 초과할 경우 HTTP 400 Bad Request")
            void updateGuestbook_WithTooLongContent_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.UpdateGuestbookRequestDTO request =
                        createUpdateRequestWithContent(LONG_CONTENT);

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.updateGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(RuntimeException.class); // 실제 validation 위치에 따라 조정
            }

            @Test
            @DisplayName("존재하지 않는 guestbookId HTTP 404 Not Found")
            void updateGuestbook_WithInvalidGuestbookId_ThrowsNotFound() {
                // given
                GuestbookRequestDTO.UpdateGuestbookRequestDTO request = createValidUpdateRequest();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findByIdAndGuestIdAndStatus(
                        INVALID_GUESTBOOK_ID, VALID_GUEST_ID, GuestbookStatus.ACTIVE))
                        .willReturn(createEmptyOptional());

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.updateGuestbook(VALID_OWNER_ID, INVALID_GUESTBOOK_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(GuestbookException.GuestbookNotFoundException.class);
            }

            @Test
            @DisplayName("타인이 작성한 방명록 수정 시 HTTP 403 Forbidden")
            void updateGuestbook_OthersGuestbook_ThrowsForbidden() {
                // given
                GuestbookRequestDTO.UpdateGuestbookRequestDTO request = createValidUpdateRequest();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(ANOTHER_USER_ID)).willReturn(createAnotherUser());
                given(guestbookRepository.findByIdAndGuestIdAndStatus(
                        VALID_GUESTBOOK_ID, ANOTHER_USER_ID, GuestbookStatus.ACTIVE))
                        .willReturn(createEmptyOptional());

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.updateGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, ANOTHER_USER_ID, request))
                        .isInstanceOf(GuestbookException.GuestbookNotFoundException.class);
            }

            @Test
            @DisplayName("삭제된 방명록 수정 시 HTTP 400 Bad Request")
            void updateGuestbook_DeletedGuestbook_ThrowsBadRequest() {
                // given
                GuestbookRequestDTO.UpdateGuestbookRequestDTO request = createValidUpdateRequest();
                Guestbook deletedGuestbook = createDeletedGuestbook();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findByIdAndGuestIdAndStatus(
                        VALID_GUESTBOOK_ID, VALID_GUEST_ID, GuestbookStatus.ACTIVE))
                        .willReturn(Optional.of(deletedGuestbook));

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.updateGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, VALID_GUEST_ID, request))
                        .isInstanceOf(GuestbookException.class)
                        .hasMessageContaining("삭제된 댓글은 수정할 수 없습니다");
            }
        }
    }

    @Nested
    @DisplayName("방명록 삭제 테스트")
    class DeleteGuestbookTest {

        @Nested
        @DisplayName("성공 시나리오")
        class SuccessScenarios {

            @Test
            @DisplayName("방명록 주인이 자신의 방명록에 있는 댓글 삭제 성공")
            void deleteGuestbook_ByOwner_Success() {
                // given
                Guestbook guestbook = createActiveGuestbook();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(guestbookRepository.findById(VALID_GUESTBOOK_ID))
                        .willReturn(Optional.of(guestbook));
                given(guestbookRepository.countActiveRepliesByTopGuestbookId(
                        VALID_GUESTBOOK_ID, GuestbookStatus.ACTIVE))
                        .willReturn(ZERO_COUNT);

                // when
                guestbookService.deleteGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, VALID_OWNER_ID);

                // then
                assertThat(guestbook.getStatus()).isEqualTo(GuestbookStatus.DEACTIVE);
                assertThat(guestbook.getDeletedAt()).isNotNull();

                verify(entityValidator, times(2)).getValidUserOrThrow(VALID_OWNER_ID);
                verify(guestbookRepository).findById(VALID_GUESTBOOK_ID);
            }

            @Test
            @DisplayName("대댓글이 있는 원댓글 삭제 시 내용만 '삭제된 댓글입니다'")
            void deleteGuestbook_ParentWithReplies_ContentDeleted() {
                // given
                Guestbook parentGuestbook = createParentGuestbookWithReplies();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findById(DeleteScenario.PARENT_WITH_REPLIES_ID))
                        .willReturn(Optional.of(parentGuestbook));
                given(guestbookRepository.countActiveRepliesByTopGuestbookId(
                        DeleteScenario.PARENT_WITH_REPLIES_ID, GuestbookStatus.ACTIVE))
                        .willReturn(ONE_COUNT);

                // when
                guestbookService.deleteGuestbook(VALID_OWNER_ID, DeleteScenario.PARENT_WITH_REPLIES_ID, VALID_GUEST_ID);

                // then
                assertThat(parentGuestbook.getStatus()).isEqualTo(GuestbookStatus.ACTIVE);
                assertThat(parentGuestbook.getContent()).isEqualTo(DELETED_COMMENT_MESSAGE);
                assertThat(parentGuestbook.getDeletedAt()).isNull();

                verify(guestbookRepository).countActiveRepliesByTopGuestbookId(
                        DeleteScenario.PARENT_WITH_REPLIES_ID, GuestbookStatus.ACTIVE);
            }

            @Test
            @DisplayName("대댓글이 없는 원댓글 삭제 시 완전 삭제")
            void deleteGuestbook_ParentWithoutReplies_FullyDeleted() {
                // given
                Guestbook guestbook = createActiveGuestbook();

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findById(VALID_GUESTBOOK_ID))
                        .willReturn(Optional.of(guestbook));
                given(guestbookRepository.countActiveRepliesByTopGuestbookId(
                        VALID_GUESTBOOK_ID, GuestbookStatus.ACTIVE))
                        .willReturn(ZERO_COUNT);

                // when
                guestbookService.deleteGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, VALID_GUEST_ID);

                // then
                assertThat(guestbook.getStatus()).isEqualTo(GuestbookStatus.DEACTIVE);
                assertThat(guestbook.getDeletedAt()).isNotNull();

                verify(guestbookRepository).countActiveRepliesByTopGuestbookId(
                        VALID_GUESTBOOK_ID, GuestbookStatus.ACTIVE);
            }
        }

        @Nested
        @DisplayName("실패 시나리오")
        class FailureScenarios {

            @Test
            @DisplayName("존재하지 않는 guestbookId 삭제 시 HTTP 404 Not Found")
            void deleteGuestbook_WithInvalidGuestbookId_ThrowsNotFound() {
                // given
                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(VALID_GUEST_ID)).willReturn(guestUser);
                given(guestbookRepository.findById(INVALID_GUESTBOOK_ID))
                        .willReturn(createEmptyOptional());

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.deleteGuestbook(VALID_OWNER_ID, INVALID_GUESTBOOK_ID, VALID_GUEST_ID))
                        .isInstanceOf(GuestbookException.GuestbookNotFoundException.class);

                verify(guestbookRepository).findById(INVALID_GUESTBOOK_ID);
            }

            @Test
            @DisplayName("작성자도 아니며, 방명록 주인도 아닌 사람이 방명록 삭제 요청 시 HTTP 403 Forbidden")
            void deleteGuestbook_WithNoPermission_ThrowsForbidden() {
                // given
                Guestbook guestbook = createActiveGuestbook(); // ownerId=1, guestId=2

                given(entityValidator.getValidUserOrThrow(VALID_OWNER_ID)).willReturn(ownerUser);
                given(entityValidator.getValidUserOrThrow(ANOTHER_USER_ID)).willReturn(createAnotherUser());
                given(guestbookRepository.findById(VALID_GUESTBOOK_ID))
                        .willReturn(Optional.of(guestbook));

                // when & then
                assertThatThrownBy(() ->
                        guestbookService.deleteGuestbook(VALID_OWNER_ID, VALID_GUESTBOOK_ID, ANOTHER_USER_ID))
                        .isInstanceOf(GuestbookException.InvalidGuestbookAccessException.class);

                verify(guestbookRepository).findById(VALID_GUESTBOOK_ID);
            }
        }
    }
}

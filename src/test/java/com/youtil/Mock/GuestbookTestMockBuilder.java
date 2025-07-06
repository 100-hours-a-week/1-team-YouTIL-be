package com.youtil.Mock;

import com.youtil.Api.Guestbook.dto.GuestbookRequestDTO;
import com.youtil.Common.Enums.GuestbookStatus;
import static com.youtil.Constants.GuestbookTestConstant.*;
import com.youtil.Model.Guestbook;
import com.youtil.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

public class GuestbookTestMockBuilder {

    public static User createValidUser(Long userId, String nickname) {
        return User.builder()
                .id(userId)
                .nickname(nickname)
                .email(nickname + "@test.com")
                .profileImageUrl(GUEST_PROFILE_URL)
                .build();
    }

    public static User createGuestUser() {
        return createValidUser(VALID_GUEST_ID, GUEST_NICKNAME);
    }

    public static User createOwnerUser() {
        return createValidUser(VALID_OWNER_ID, OWNER_NICKNAME);
    }

    public static User createAnotherUser() {
        return createValidUser(ANOTHER_USER_ID, "다른사용자");
    }

    public static Guestbook.GuestbookBuilder createBaseGuestbookBuilder() {
        return Guestbook.builder()
                .id(VALID_GUESTBOOK_ID)
                .ownerId(VALID_OWNER_ID)
                .guestId(VALID_GUEST_ID)
                .content(VALID_CONTENT)
                .status(GuestbookStatus.ACTIVE)
                .createdAt(CREATED_AT)
                .updatedAt(UPDATED_AT)
                .guest(createGuestUser());
    }

    public static Guestbook createActiveGuestbook() {
        return createBaseGuestbookBuilder().build();
    }

    public static Guestbook createActiveGuestbookWithId(Long id) {
        return createBaseGuestbookBuilder()
                .id(id)
                .build();
    }

    public static Guestbook createReplyGuestbook(Long parentId) {
        return createBaseGuestbookBuilder()
                .id(VALID_GUESTBOOK_ID + 1)
                .topGuestbookId(parentId)
                .content(CreateScenario.REPLY_CONTENT)
                .build();
    }

    public static Guestbook createDeletedGuestbook() {
        return createBaseGuestbookBuilder()
                .content(DELETED_COMMENT_MESSAGE)
                .build();
    }

    public static Guestbook createSoftDeletedGuestbook() {
        return createBaseGuestbookBuilder()
                .status(GuestbookStatus.DEACTIVE)
                .deletedAt(DELETED_AT)
                .build();
    }

    public static GuestbookRequestDTO.CreateGuestbookRequestDTO createValidCreateRequest() {
        GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                mock(GuestbookRequestDTO.CreateGuestbookRequestDTO.class);
        lenient().when(request.getContent()).thenReturn(CreateScenario.SUCCESS_CONTENT);
        lenient().when(request.getTopGuestbookId()).thenReturn(null);
        return request;
    }

    public static GuestbookRequestDTO.CreateGuestbookRequestDTO createReplyRequest(Long parentId) {
        GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                mock(GuestbookRequestDTO.CreateGuestbookRequestDTO.class);
        lenient().when(request.getContent()).thenReturn(CreateScenario.REPLY_CONTENT);
        lenient().when(request.getTopGuestbookId()).thenReturn(parentId);
        return request;
    }

    public static GuestbookRequestDTO.CreateGuestbookRequestDTO createRequestWithContent(String content) {
        GuestbookRequestDTO.CreateGuestbookRequestDTO request =
                mock(GuestbookRequestDTO.CreateGuestbookRequestDTO.class);
        lenient().when(request.getContent()).thenReturn(content);
        lenient().when(request.getTopGuestbookId()).thenReturn(null);
        return request;
    }

    public static GuestbookRequestDTO.UpdateGuestbookRequestDTO createValidUpdateRequest() {
        GuestbookRequestDTO.UpdateGuestbookRequestDTO request =
                mock(GuestbookRequestDTO.UpdateGuestbookRequestDTO.class);
        lenient().when(request.getContent()).thenReturn(UpdateScenario.NEW_CONTENT);
        return request;
    }

    public static GuestbookRequestDTO.UpdateGuestbookRequestDTO createUpdateRequestWithContent(String content) {
        GuestbookRequestDTO.UpdateGuestbookRequestDTO request =
                mock(GuestbookRequestDTO.UpdateGuestbookRequestDTO.class);
        lenient().when(request.getContent()).thenReturn(content);
        return request;
    }

    public static Page<Guestbook> createEmptyGuestbookPage() {
        Pageable pageable = PageRequest.of(VALID_PAGE, VALID_SIZE);
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }


    public static Page<Guestbook> createSingleGuestbookPageWithGuestbook(Guestbook guestbook) {
        List<Guestbook> content = Arrays.asList(guestbook);
        Pageable pageable = PageRequest.of(VALID_PAGE, VALID_SIZE);
        return new PageImpl<>(content, pageable, 1);
    }

    public static Page<Guestbook> createMultipleGuestbookPage() {
        List<Guestbook> content = Arrays.asList(
                createActiveGuestbookWithId(1L),
                createActiveGuestbookWithId(2L),
                createActiveGuestbookWithId(3L)
        );
        Pageable pageable = PageRequest.of(VALID_PAGE, VALID_SIZE);
        return new PageImpl<>(content, pageable, 3);
    }

    public static List<Guestbook> createEmptyRepliesList() {
        return new ArrayList<>();
    }

    public static Optional<Guestbook> createOptionalGuestbookWithId(Long id) {
        return Optional.of(createActiveGuestbookWithId(id));
    }

    public static Optional<Guestbook> createEmptyOptional() {
        return Optional.empty();
    }

    public static Pageable createValidPageable() {
        return PageRequest.of(VALID_PAGE, VALID_SIZE);
    }

    public static Guestbook createParentGuestbookWithReplies() {
        Guestbook parent = createActiveGuestbookWithId(DeleteScenario.PARENT_WITH_REPLIES_ID);
        parent.setContent(DeleteScenario.BEFORE_DELETE_CONTENT);
        return parent;
    }
}

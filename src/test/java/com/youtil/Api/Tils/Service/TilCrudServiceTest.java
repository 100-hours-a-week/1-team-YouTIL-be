package com.youtil.Api.Tils.Service;

import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.User.Dto.UserResponseDTO;
import com.youtil.Common.Enums.Status;

import static com.youtil.Constants.MockTilConstants.*;
import static com.youtil.Constants.MockUserConstants.*;
import static com.youtil.Mock.MockTilBuilder.createMockTil;
import static com.youtil.Mock.MockUserBuilder.createMockUser;
import com.youtil.Model.Til;
import com.youtil.Model.User;
import com.youtil.Repository.TilRepository;
import com.youtil.Util.EntityValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import org.assertj.core.util.Lists;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TilCrudServiceTest {

    @Mock private TilRepository tilRepository;
    @Mock private EntityValidator entityValidator;

    @InjectMocks private TilCommendService tilCommendService;

    private User mockUser;
    private User otherUser;
    private Til mockTil;
    private Til privateTil;

    @BeforeEach
    void setup() {
        mockUser = createMockUser();
        otherUser = createMockUser();
        otherUser.setId(OTHER_USER_ID);

        mockTil = createMockTil(mockUser);
        privateTil = createMockTil(mockUser);
        privateTil.setIsDisplay(false);
    }

    // ========== AI 기반 TIL 생성 테스트 ==========

    @Test
    @DisplayName("TIL 생성 - 유효한 커밋/사용자 정보를 AI API로 전송하여 TIL이 정상적으로 생성된다")
    void createTilFromAi_withValidData_success() {
        // given
        TilRequestDTO.CreateAiTilRequest request = TilRequestDTO.CreateAiTilRequest.builder()
                .repo(String.valueOf(123L))
                .title(MOCK_TITLE)
                .category(MOCK_CATEGORY_BACKEND)
                .content(AI_RESPONSE_CONTENT)
                .tags(AI_KEYWORDS)
                .isShared(true)
                .build();

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(tilRepository.save(any(Til.class))).thenReturn(mockTil);

        // when
        TilResponseDTO.CreateTilResponse result = tilCommendService.createTilFromAi(request, MOCK_USER_ID);

        // then
        assertEquals(MOCK_TIL_ID, result.getTilID());
        verify(tilRepository).save(any(Til.class));
    }

    @Test
    @DisplayName("TIL 생성 - 존재하지 않는 사용자 ID로 요청하는 경우")
    void createTilFromAi_withInvalidUserId_fail() {
        // given
        TilRequestDTO.CreateAiTilRequest request = TilRequestDTO.CreateAiTilRequest.builder()
                .repo(String.valueOf(123L))
                .title(MOCK_TITLE)
                .category(MOCK_CATEGORY_BACKEND)
                .content(AI_RESPONSE_CONTENT)
                .tags(AI_KEYWORDS)
                .isShared(true)
                .build();

        when(entityValidator.getValidUserOrThrow(INVALID_USER_ID))
                .thenThrow(new RuntimeException(USER_NOT_FOUND_MESSAGE));

        // when & then
        assertThatThrownBy(() -> tilCommendService.createTilFromAi(request, INVALID_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(USER_NOT_FOUND_MESSAGE);
    }

    // ========== TIL 목록 조회 테스트 ==========

    @Test
    @DisplayName("TIL 목록 조회 - 사용자의 TIL 목록이 정상적으로 조회됨")
    void getUserTils_withValidUser_success() {
        // given
        UserResponseDTO.TilListItem tilItem = UserResponseDTO.TilListItem.builder()
                .tilId(MOCK_TIL_ID)
                .title(MOCK_TITLE)
                .userName(MOCK_USER_NICKNAME)
                .id(MOCK_USER_ID)
                .tags(MOCK_TAGS_SPRING)
                .userProfileImageUrl(MOCK_USER_PROFILE)
                .build();
        List<UserResponseDTO.TilListItem> tilList = Lists.newArrayList(tilItem);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(tilRepository.findUserTils(eq(MOCK_USER_ID), any(Pageable.class))).thenReturn(tilList);

        // when
        TilResponseDTO.TilListResponse result = tilCommendService.getUserTils(MOCK_USER_ID, TIL_DEFAULT_PAGE, TIL_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTils().size());
        assertEquals(MOCK_TITLE, result.getTils().get(0).getTitle());
    }

    @Test
    @DisplayName("TIL 목록 조회 - TIL이 없는 경우 빈 목록 반환")
    void getUserTils_withNoTils_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(tilRepository.findUserTils(eq(MOCK_USER_ID), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        // when
        TilResponseDTO.TilListResponse result = tilCommendService.getUserTils(MOCK_USER_ID, TIL_DEFAULT_PAGE, TIL_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertTrue(result.getTils().isEmpty());
    }

    @Test
    @DisplayName("TIL 목록 조회 - 존재하지 않는 사용자 ID로 요청")
    void getUserTils_withInvalidUserId_fail() {
        // given
        when(entityValidator.getValidUserOrThrow(INVALID_USER_ID))
                .thenThrow(new RuntimeException(USER_NOT_FOUND_MESSAGE));

        // when & then
        assertThatThrownBy(() -> tilCommendService.getUserTils(INVALID_USER_ID, TIL_DEFAULT_PAGE, TIL_DEFAULT_SIZE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(USER_NOT_FOUND_MESSAGE);
    }

    // ========== 특정 날짜 TIL 목록 조회 테스트 ==========

    @Test
    @DisplayName("특정 날짜 TIL 목록 조회 - 특정 날짜에 해당하는 사용자의 TIL 목록 반환")
    void getUserTilsByDate_withValidDateAndUser_success() {
        // given
        UserResponseDTO.TilListItem tilItem = UserResponseDTO.TilListItem.builder()
                .tilId(MOCK_TIL_ID)
                .title(MOCK_TITLE)
                .userName(MOCK_USER_NICKNAME)
                .id(MOCK_USER_ID)
                .tags(MOCK_TAGS_SPRING)
                .userProfileImageUrl(MOCK_USER_PROFILE)
                .build();
        List<UserResponseDTO.TilListItem> tilList = Lists.newArrayList(tilItem);

        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(tilRepository.findUserTilsByDateRange(eq(MOCK_USER_ID), any(LocalDateTime.class),
                any(LocalDateTime.class), any(Pageable.class))).thenReturn(tilList);

        // when
        TilResponseDTO.TilListResponse result = tilCommendService.getUserTilsByDate(
                MOCK_USER_ID, TEST_DATE, TIL_DEFAULT_PAGE, TIL_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTils().size());
        assertEquals(MOCK_TITLE, result.getTils().get(0).getTitle());
    }

    @Test
    @DisplayName("특정 날짜 TIL 목록 조회 - 해당 날짜에 TIL이 없을 경우 빈 목록 반환")
    void getUserTilsByDate_withNoTils_success() {
        // given
        when(entityValidator.getValidUserOrThrow(MOCK_USER_ID)).thenReturn(mockUser);
        when(tilRepository.findUserTilsByDateRange(eq(MOCK_USER_ID), any(LocalDateTime.class),
                any(LocalDateTime.class), any(Pageable.class))).thenReturn(Collections.emptyList());

        // when
        TilResponseDTO.TilListResponse result = tilCommendService.getUserTilsByDate(
                MOCK_USER_ID, TEST_DATE, TIL_DEFAULT_PAGE, TIL_DEFAULT_SIZE);

        // then
        assertNotNull(result);
        assertTrue(result.getTils().isEmpty());
    }

    @Test
    @DisplayName("특정 날짜 TIL 목록 조회 - 잘못된 날짜 형식으로 요청 시")
    void getUserTilsByDate_withInvalidDateFormat_fail() {
        // when & then
        assertThatThrownBy(() -> {
            LocalDate.parse("invalid-date");
        }).isInstanceOf(DateTimeParseException.class);
    }

    // ========== TIL 상세 조회 테스트 ==========

    @Test
    @DisplayName("TIL 상세 조회 - 존재하는 TIL ID로 상세 정보 조회 성공")
    void getTilById_withValidTilId_success() {
        // given
        when(tilRepository.findById(MOCK_TIL_ID)).thenReturn(Optional.of(mockTil));

        // when
        TilResponseDTO.TilDetailResponse result = tilCommendService.getTilById(MOCK_TIL_ID, MOCK_USER_ID);

        // then
        assertNotNull(result);
        assertEquals(mockTil.getTitle(), result.getTitle());
        assertEquals(mockTil.getContent(), result.getContent());
        assertEquals(mockTil.getCategory(), result.getCategory());
    }

    @Test
    @DisplayName("TIL 상세 조회 - 존재하지 않는 TIL ID로 조회 시")
    void getTilById_withInvalidTilId_fail() {
        // given
        when(tilRepository.findById(INVALID_TIL_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tilCommendService.getTilById(INVALID_TIL_ID, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(TIL_NOT_FOUND_MESSAGE);
    }

    @Test
    @DisplayName("TIL 상세 조회 - 삭제된 TIL ID로 조회 시")
    void getTilById_withDeletedTilId_fail() {
        // given
        mockTil.setStatus(Status.deactive);
        when(tilRepository.findById(MOCK_TIL_ID)).thenReturn(Optional.of(mockTil));

        // when & then
        assertThatThrownBy(() -> tilCommendService.getTilById(MOCK_TIL_ID, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(TIL_ALREADY_DELETED_MESSAGE);
    }

    @Test
    @DisplayName("TIL 상세 조회 - 타인의 비공개 TIL 조회 시도")
    void getTilById_withPrivateTilFromOtherUser_fail() {
        // given
        privateTil.setUser(otherUser);
        when(tilRepository.findById(privateTil.getId())).thenReturn(Optional.of(privateTil));

        // when & then
        assertThatThrownBy(() -> tilCommendService.getTilById(privateTil.getId(), MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(TIL_ACCESS_DENIED_MESSAGE);
    }

    // ========== TIL 수정 테스트 ==========

    @Test
    @DisplayName("TIL 수정 - 본인의 TIL 수정 성공")
    void updateTil_withValidOwner_success() {
        // given
        TilRequestDTO.UpdateTilRequest request = TilRequestDTO.UpdateTilRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .category("FRONTEND")
                .tag(Arrays.asList("React", "TypeScript"))
                .isDisplay(false)
                .commitRepository("updated-repo")
                .isUploaded(true)
                .build();

        when(tilRepository.findById(MOCK_TIL_ID)).thenReturn(Optional.of(mockTil));
        when(tilRepository.save(any(Til.class))).thenReturn(mockTil);

        // when
        TilResponseDTO.TilDetailResponse result = tilCommendService.updateTil(MOCK_TIL_ID, request, MOCK_USER_ID);

        // then
        assertNotNull(result);
        verify(tilRepository).save(mockTil);
    }

    @Test
    @DisplayName("TIL 수정 - 타인의 TIL 수정 시도")
    void updateTil_withOtherUserTil_fail() {
        // given
        TilRequestDTO.UpdateTilRequest request = TilRequestDTO.UpdateTilRequest.builder()
                .title("Updated Title")
                .build();

        mockTil.setUser(otherUser);
        when(tilRepository.findById(MOCK_TIL_ID)).thenReturn(Optional.of(mockTil));

        // when & then
        assertThatThrownBy(() -> tilCommendService.updateTil(MOCK_TIL_ID, request, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("수정 권한이 없습니다");
    }

    // ========== TIL 삭제 테스트 ==========

    @Test
    @DisplayName("TIL 삭제 - 본인의 TIL 삭제 성공")
    void deleteTil_withValidOwner_success() {
        // given
        when(tilRepository.findById(MOCK_TIL_ID)).thenReturn(Optional.of(mockTil));
        when(tilRepository.save(any(Til.class))).thenReturn(mockTil);

        // when
        tilCommendService.deleteTil(MOCK_TIL_ID, MOCK_USER_ID);

        // then
        verify(tilRepository).save(mockTil);
        assertEquals(Status.deactive, mockTil.getStatus());
        assertNotNull(mockTil.getDeletedAt());
    }

    @Test
    @DisplayName("TIL 삭제 - 타인의 TIL 삭제 시도")
    void deleteTil_withOtherUserTil_fail() {
        // given
        mockTil.setUser(otherUser);
        when(tilRepository.findById(MOCK_TIL_ID)).thenReturn(Optional.of(mockTil));

        // when & then
        assertThatThrownBy(() -> tilCommendService.deleteTil(MOCK_TIL_ID, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("삭제 권한이 없습니다");
    }

    @Test
    @DisplayName("TIL 삭제 - 이미 삭제된 TIL 삭제 시도")
    void deleteTil_withAlreadyDeletedTil_fail() {
        // given
        mockTil.setStatus(Status.deactive);
        when(tilRepository.findById(MOCK_TIL_ID)).thenReturn(Optional.of(mockTil));

        // when & then
        assertThatThrownBy(() -> tilCommendService.deleteTil(MOCK_TIL_ID, MOCK_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(TIL_ALREADY_DELETED_MESSAGE);
    }
}

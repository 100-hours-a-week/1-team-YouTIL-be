package com.youtil.Api.Tils.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Controller.TilUpdateDeleteController;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Constants.TilTestConstants;
import com.youtil.Mock.TilMockBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({TilUpdateDeleteController.class})
@DisplayName("TIL 수정/삭제 API 테스트")
class TilUpdateDeleteApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TilCommendService tilCommendService;

    private TilRequestDTO.UpdateTilRequest validUpdateRequest;
    private TilResponseDTO.TilDetailResponse tilDetailResponse;

    @BeforeEach
    void setUp() {
        validUpdateRequest = TilMockBuilder.createUpdateTilRequest();
        tilDetailResponse = TilMockBuilder.createTilDetailResponse();
    }

    @Nested
    @DisplayName("TIL 수정")
    class TilUpdateTest {

        @Nested
        @DisplayName("성공 시나리오")
        class SuccessScenarios {

            @Test
            @DisplayName("유효한 tilId, title로 요청 시 TIL 제목 수정 성공")
            @WithMockUser(username = "1")
            void updateTil_ValidRequest_Success() throws Exception {
                // given
                given(tilCommendService.getTilById(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(tilDetailResponse);
                given(tilCommendService.updateTil(eq(TilTestConstants.TEST_TIL_ID), any(), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(tilDetailResponse);

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_200))
                        .andExpect(jsonPath("$.message").value(TilTestConstants.SUCCESS_TIL_UPDATED))
                        .andExpect(jsonPath("$.data.id").value(TilTestConstants.TEST_TIL_ID))
                        .andExpect(jsonPath("$.data.title").value(TilTestConstants.TEST_TIL_TITLE))
                        .andExpect(jsonPath("$.data.userId").value(TilTestConstants.TEST_USER_ID));
            }

            @Test
            @DisplayName("본인이 작성한 TIL만 수정 성공")
            @WithMockUser(username = "1")
            void updateTil_OwnerRequest_Success() throws Exception {
                // given
                given(tilCommendService.getTilById(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(tilDetailResponse);
                given(tilCommendService.updateTil(eq(TilTestConstants.TEST_TIL_ID), any(), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(tilDetailResponse);

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest)))
                        .andExpect(status().isOk());

                then(tilCommendService).should().updateTil(
                        eq(TilTestConstants.TEST_TIL_ID),
                        argThat(request -> request.getTitle().equals(TilTestConstants.UPDATED_TIL_TITLE)),
                        eq(TilTestConstants.TEST_USER_ID)
                );
            }

            @Test
            @DisplayName("40자 이내의 제목으로 수정 시 수정 성공")
            @WithMockUser(username = "1")
            void updateTil_ValidTitleLength_Success() throws Exception {
                // given
                TilRequestDTO.UpdateTilRequest request = TilMockBuilder.createUpdateTilRequestWith40CharsTitle();
                TilResponseDTO.TilDetailResponse response = TilMockBuilder.createTilDetailResponse();

                given(tilCommendService.getTilById(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(response);
                given(tilCommendService.updateTil(eq(TilTestConstants.TEST_TIL_ID), any(), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(response);

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.title").value(TilTestConstants.TEST_TIL_TITLE));

                then(tilCommendService).should().updateTil(
                        eq(TilTestConstants.TEST_TIL_ID),
                        argThat(req -> req.getTitle().equals(TilTestConstants.VALID_TITLE_40_CHARS)),
                        eq(TilTestConstants.TEST_USER_ID)
                );
            }

            @Test
            @DisplayName("수정 후 updatedAt 수정")
            @WithMockUser(username = "1")
            void updateTil_UpdatedAtChanged() throws Exception {
                // given
                given(tilCommendService.getTilById(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(tilDetailResponse);
                given(tilCommendService.updateTil(eq(TilTestConstants.TEST_TIL_ID), any(), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(tilDetailResponse);

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.updatedAt").exists())
                        .andExpect(jsonPath("$.data.title").value(TilTestConstants.TEST_TIL_TITLE))
                        .andExpect(jsonPath("$.data.id").value(TilTestConstants.TEST_TIL_ID));
            }
        }

        @Nested
        @DisplayName("실패 시나리오")
        class FailureScenarios {

            @Test
            @DisplayName("tilId null 전달 시 HTTP 400 Bad Request")
            @WithMockUser(username = "1")
            void updateTil_NullTilId_BadRequest() throws Exception {
                // given
                TilRequestDTO.UpdateTilRequest request = TilMockBuilder.createUpdateTilRequestWithNullTilId();

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_400))
                        .andExpect(jsonPath("$.success").value(false));
            }

            @Test
            @DisplayName("title null 전달 시 HTTP 400 Bad Request")
            @WithMockUser(username = "1")
            void updateTil_NullTitle_BadRequest() throws Exception {
                // given
                TilRequestDTO.UpdateTilRequest request = TilMockBuilder.createUpdateTilRequestWithNullTitle();

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_400))
                        .andExpect(jsonPath("$.success").value(false));
            }

            @Test
            @DisplayName("title 빈 문자열 전달 시 HTTP 400 Bad Request")
            @WithMockUser(username = "1")
            void updateTil_EmptyTitle_BadRequest() throws Exception {
                // given
                TilRequestDTO.UpdateTilRequest request = TilMockBuilder.createUpdateTilRequest(
                        TilTestConstants.TEST_TIL_ID, ""
                );

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_400));
            }

            @Test
            @DisplayName("title 공백만 포함 시 HTTP 400 Bad Request")
            @WithMockUser(username = "1")
            void updateTil_WhitespaceOnlyTitle_BadRequest() throws Exception {
                // given
                TilRequestDTO.UpdateTilRequest request = TilMockBuilder.createUpdateTilRequest(
                        TilTestConstants.TEST_TIL_ID, "   "
                );

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_400));
            }

            @Test
            @DisplayName("tilId 0 이하 값 전달 시 HTTP 500 Internal Server Error")
            @WithMockUser(username = "1")
            void updateTil_InvalidTilId_InternalServerError() throws Exception {
                // given
                TilRequestDTO.UpdateTilRequest request = TilMockBuilder.createUpdateTilRequest(
                        0L, TilTestConstants.UPDATED_TIL_TITLE
                );

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500));
            }

            @Test
            @DisplayName("title 40자 초과 시 HTTP 500 Internal Server Error")
            @WithMockUser(username = "1")
            void updateTil_TitleTooLong_InternalServerError() throws Exception {
                // given
                TilRequestDTO.UpdateTilRequest request = TilMockBuilder.createUpdateTilRequestWithLongTitle();

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                        .andExpect(jsonPath("$.success").value(false));
            }

            @Test
            @DisplayName("존재하지 않는 tilId 요청 시 HTTP 404 Not Found")
            @WithMockUser(username = "1")
            void updateTil_TilNotFound_NotFound() throws Exception {
                // given
                given(tilCommendService.getTilById(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID)))
                        .willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_NOT_FOUND));

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_404))
                        .andExpect(jsonPath("$.message").value(TilTestConstants.ERROR_TIL_NOT_FOUND));
            }

            @Test
            @DisplayName("타인의 TIL 수정 시도 시 HTTP 403 Forbidden")
            @WithMockUser(username = "1")
            void updateTil_NotOwner_Forbidden() throws Exception {
                // given
                given(tilCommendService.getTilById(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(tilDetailResponse);
                given(tilCommendService.updateTil(eq(TilTestConstants.TEST_TIL_ID), any(), eq(TilTestConstants.TEST_USER_ID)))
                        .willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_EDIT_DENIED));

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_403))
                        .andExpect(jsonPath("$.message").value(TilTestConstants.ERROR_TIL_EDIT_DENIED));

                then(tilCommendService).should().updateTil(
                        eq(TilTestConstants.TEST_TIL_ID),
                        argThat(request -> request.getTitle().equals(TilTestConstants.UPDATED_TIL_TITLE)),
                        eq(TilTestConstants.TEST_USER_ID)
                );
            }

            @Test
            @DisplayName("이미 삭제된 TIL 수정 시도 시 HTTP 410 Gone")
            @WithMockUser(username = "1")
            void updateTil_DeletedTil_Gone() throws Exception {
                // given
                given(tilCommendService.getTilById(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID)))
                        .willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_ALREADY_DELETED));

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest)))
                        .andExpect(status().isGone())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_410))
                        .andExpect(jsonPath("$.message").value(TilTestConstants.ERROR_TIL_ALREADY_DELETED));
            }

            @Test
            @DisplayName("서버 내부 오류 시 HTTP 500 Internal Server Error")
            @WithMockUser(username = "1")
            void updateTil_ServerError_InternalServerError() throws Exception {
                // given
                given(tilCommendService.getTilById(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID)))
                        .willReturn(tilDetailResponse);
                given(tilCommendService.updateTil(eq(TilTestConstants.TEST_TIL_ID), any(), eq(TilTestConstants.TEST_USER_ID)))
                        .willThrow(new RuntimeException("예상치 못한 서버 오류"));

                // when & then
                mockMvc.perform(put("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUpdateRequest)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                        .andExpect(jsonPath("$.success").value(false));
            }
        }
    }

    @Nested
    @DisplayName("TIL 삭제")
    class TilDeleteTest {

        @Nested
        @DisplayName("성공 시나리오")
        class SuccessScenarios {

            @Test
            @DisplayName("유효한 tilId 배열로 요청 시 일괄 삭제")
            @WithMockUser(username = "1")
            void deleteTils_ValidRequest_Success() throws Exception {
                // given
                TilRequestDTO.BatchDeleteTilRequest request = TilMockBuilder.createBatchDeleteTilRequest();

                willDoNothing().given(tilCommendService).deleteTil(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID));

                // when & then
                mockMvc.perform(delete("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_200))
                        .andExpect(jsonPath("$.success").value(true));

                then(tilCommendService).should().deleteTil(
                        eq(TilTestConstants.TEST_TIL_ID),
                        eq(TilTestConstants.TEST_USER_ID)
                );
            }

            @Test
            @DisplayName("deletedAt 시간 설정")
            @WithMockUser(username = "1")
            void deleteTils_DeletedAtSet() throws Exception {
                // given
                TilRequestDTO.BatchDeleteTilRequest request = TilMockBuilder.createBatchDeleteTilRequest();

                willDoNothing().given(tilCommendService).deleteTil(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID));

                // when & then
                mockMvc.perform(delete("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk());

                then(tilCommendService).should().deleteTil(
                        eq(TilTestConstants.TEST_TIL_ID),
                        eq(TilTestConstants.TEST_USER_ID)
                );
            }
        }

        @Nested
        @DisplayName("실패 시나리오")
        class FailureScenarios {

            @Test
            @DisplayName("tilId null 전달 시 HTTP 400 Bad Request")
            @WithMockUser(username = "1")
            void deleteTils_NullTilIds_BadRequest() throws Exception {
                // given
                TilRequestDTO.BatchDeleteTilRequest request = TilMockBuilder.createBatchDeleteTilRequestWithNullIds();

                // when & then
                mockMvc.perform(delete("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_400))
                        .andExpect(jsonPath("$.success").value(false));
            }

            @Test
            @DisplayName("빈 배열 전달 시 HTTP 400 Bad Request")
            @WithMockUser(username = "1")
            void deleteTils_EmptyTilIds_BadRequest() throws Exception {
                // given
                TilRequestDTO.BatchDeleteTilRequest request = TilMockBuilder.createBatchDeleteTilRequestWithEmptyIds();

                // when & then
                mockMvc.perform(delete("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_400))
                        .andExpect(jsonPath("$.success").value(false));
            }

            @Test
            @DisplayName("존재하지 않는 tilId 포함 시 '삭제하려는 TIL을 찾을 수 없습니다' 오류")
            @WithMockUser(username = "1")
            void deleteTils_NonExistentTilId_Error() throws Exception {
                // given
                TilRequestDTO.BatchDeleteTilRequest request = TilMockBuilder.createBatchDeleteTilRequestWithNonExistentId();

                willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_NOT_FOUND))
                        .given(tilCommendService).deleteTil(eq(TilTestConstants.NON_EXISTENT_TIL_ID), eq(TilTestConstants.TEST_USER_ID));

                // when & then
                mockMvc.perform(delete("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                        .andExpect(jsonPath("$.message").value(TilTestConstants.ERROR_TIL_NOT_FOUND))
                        .andExpect(jsonPath("$.success").value(false));
            }

            @Test
            @DisplayName("타인의 TIL 삭제 시도 시 '해당 TIL의 소유자가 아닙니다' 오류")
            @WithMockUser(username = "1")
            void deleteTils_NotOwner_Error() throws Exception {
                // given
                TilRequestDTO.BatchDeleteTilRequest request = TilMockBuilder.createBatchDeleteTilRequest();

                willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_DELETE_DENIED))
                        .given(tilCommendService).deleteTil(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID));

                // when & then
                mockMvc.perform(delete("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                        .andExpect(jsonPath("$.message").value(TilTestConstants.ERROR_TIL_DELETE_DENIED))
                        .andExpect(jsonPath("$.success").value(false));
            }

            @Test
            @DisplayName("이미 삭제된 TIL 포함 시 '이미 삭제된 TIL이 포함되어 있습니다' 오류")
            @WithMockUser(username = "1")
            void deleteTils_AlreadyDeletedTil_Error() throws Exception {
                // given
                TilRequestDTO.BatchDeleteTilRequest request = TilMockBuilder.createBatchDeleteTilRequest();

                willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_ALREADY_DELETED))
                        .given(tilCommendService).deleteTil(eq(TilTestConstants.TEST_TIL_ID), eq(TilTestConstants.TEST_USER_ID));

                // when & then
                mockMvc.perform(delete("/api/v1/tils")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                        .andExpect(jsonPath("$.message").value(TilTestConstants.ERROR_TIL_ALREADY_DELETED))
                        .andExpect(jsonPath("$.success").value(false));
            }
        }
    }
}

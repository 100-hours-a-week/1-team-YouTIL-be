package com.youtil.Api.Tils.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Controller.TilCreateController;
import com.youtil.Api.Tils.Dto.TilRequestDTO;
import com.youtil.Api.Tils.Dto.TilResponseDTO;
import com.youtil.Api.Tils.Queue.TilQueueProducer;
import com.youtil.Common.Constants.AiServiceConstants;
import com.youtil.Constants.TilTestConstants;
import com.youtil.Mock.TilMockBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({TilCreateController.class})
@DisplayName("TIL 생성 API 테스트")
class TilCreateApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TilQueueProducer tilQueueProducer;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private AiServiceConstants tilServiceConstants;

    private TilRequestDTO.CreateWithAiRequest validCreateRequest;
    private TilResponseDTO.CreateTilResponse successResponse;

    @BeforeEach
    void setUp() {
        validCreateRequest = TilMockBuilder.createValidCreateWithAiRequest();
        successResponse = TilMockBuilder.createSuccessCreateTilResponse();

        // Redis 관련 Mock 설정
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(tilServiceConstants.getResultKey()).willReturn("ai:til:result");
        given(tilServiceConstants.getResendTimeoutSeconds()).willReturn(10);
    }

    @Nested
    @DisplayName("성공 시나리오")
    class SuccessScenarios {

        @Test
        @DisplayName("유효한 커밋 / 사용자 정보를 AI api 로 전송하여 TIL이 정상적으로 생성된다")
        @WithMockUser(username = "1")
        void createTil_ValidCommitAndUserInfo_Success() throws Exception {
            // given
            String requestId = "test-request-id";
            String resultJson = objectMapper.writeValueAsString(successResponse);

            given(tilQueueProducer.enqueueTilRequest(eq(TilTestConstants.TEST_USER_ID), any()))
                    .willReturn(requestId);
            given(valueOperations.get(anyString()))
                    .willReturn(null) // 첫 번째 호출에서는 null
                    .willReturn(resultJson); // 두 번째 호출에서는 결과 반환

            // when & then
            mockMvc.perform(post("/api/v1/tils")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_201))
                    .andExpect(jsonPath("$.message").value(TilTestConstants.SUCCESS_TIL_CREATED))
                    .andExpect(jsonPath("$.data.tilID").value(TilTestConstants.TEST_TIL_ID));

            // 큐에 요청이 전송되었는지 확인
            then(tilQueueProducer).should().enqueueTilRequest(
                    eq(TilTestConstants.TEST_USER_ID),
                    argThat(request ->
                            request.getRepositoryId().equals(TilTestConstants.GITHUB_REPOSITORY_ID) &&
                                    request.getTitle().equals(TilTestConstants.TEST_TIL_TITLE) &&
                                    request.getCategory().equals(TilTestConstants.TEST_TIL_CATEGORY)
                    )
            );
        }
    }

    @Nested
    @DisplayName("실패 시나리오")
    class FailureScenarios {

        @Test
        @DisplayName("AI 서버 연결 실패")
        @WithMockUser(username = "1")
        void createTil_AiServerConnectionFailed() throws Exception {
            // given
            given(tilQueueProducer.enqueueTilRequest(any(), any()))
                    .willThrow(new RuntimeException(TilTestConstants.ERROR_AI_CONNECTION_FAILED));

            // when & then
            mockMvc.perform(post("/api/v1/tils")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("AI 서버 응답 오류")
        @WithMockUser(username = "1")
        void createTil_AiServerResponseError() throws Exception {
            // given
            String requestId = "test-request-id";

            given(tilQueueProducer.enqueueTilRequest(any(), any())).willReturn(requestId);
            given(valueOperations.get(anyString()))
                    .willThrow(new RuntimeException("AI 서버 응답 오류"));

            // when & then
            mockMvc.perform(post("/api/v1/tils")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("빈 응답 반환")
        @WithMockUser(username = "1")
        void createTil_EmptyResponse() throws Exception {
            // given
            String requestId = "test-request-id";

            given(tilQueueProducer.enqueueTilRequest(any(), any())).willReturn(requestId);
            given(valueOperations.get(anyString())).willReturn(null); // 빈 응답

            // when & then
            mockMvc.perform(post("/api/v1/tils")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 요청하는 경우")
        @WithMockUser(username = "999")
        void createTil_NonExistentUserId() throws Exception {
            // given
            given(tilQueueProducer.enqueueTilRequest(eq(TilTestConstants.NON_EXISTENT_USER_ID), any()))
                    .willThrow(new RuntimeException(TilTestConstants.ERROR_USER_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/v1/tils")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}

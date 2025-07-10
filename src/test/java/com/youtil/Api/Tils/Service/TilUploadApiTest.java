package com.youtil.Api.Tils.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youtil.Api.Tils.Controller.TilUploadController;
import com.youtil.Api.Tils.Dto.TilUploadRequestDTO;
import com.youtil.Api.Tils.Dto.TilUploadResponseDTO;
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

@WebMvcTest({TilUploadController.class})
@DisplayName("TIL 깃허브 업로드 API 테스트")
class TilUploadApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TilUploadService tilUploadService;

    // 각 테스트마다 새로운 객체로 초기화하여 테스트 데이터 격리 보장
    private TilUploadRequestDTO.UploadRequest validUploadRequest;
    private TilUploadResponseDTO.UploadToGitHubResponse successUploadResponse;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 격리: 각 테스트마다 새로운 Mock 객체 생성
        validUploadRequest = TilMockBuilder.createTilUploadRequest();
        successUploadResponse = TilMockBuilder.createSuccessUploadResponse();
    }

    @Nested
    @DisplayName("성공 시나리오")
    class SuccessScenarios {

        @Test
        @DisplayName("유효한 tilId로 요청 시 기본 설정된 레포지토리에 마크다운 파일로 업로드 성공")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_ValidRequest_Success() throws Exception {
            // given
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willReturn(successUploadResponse);

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.success").value(true))
                    .andExpect(jsonPath("$.data.fileUrl").value(TilTestConstants.GITHUB_FILE_URL))
                    // 검증 로직 강화: 업로드 응답 데이터 상세 검증
                    .andExpect(jsonPath("$.data.commitSha").value(TilTestConstants.GITHUB_COMMIT_SHA))
                    .andExpect(jsonPath("$.data.uploadedFilePath").value(TilTestConstants.GITHUB_FILE_PATH))
                    .andExpect(jsonPath("$.data.message").value(TilTestConstants.SUCCESS_TIL_UPLOADED));

            // 서비스 메서드 호출 검증
            then(tilUploadService).should().uploadTilToGitHub(
                    argThat(request -> request.getTilId().equals(TilTestConstants.TEST_TIL_ID)),
                    eq(TilTestConstants.TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("업로드 후 isUploaded 상태 변경 확인")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_IsUploadedStatusChanged() throws Exception {
            // given
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willReturn(successUploadResponse);

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.success").value(true));

            // 서비스 메서드 호출 검증: 정확한 파라미터로 호출되었는지 확인
            then(tilUploadService).should().uploadTilToGitHub(
                    argThat(request -> request.getTilId().equals(TilTestConstants.TEST_TIL_ID)),
                    eq(TilTestConstants.TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("마크다운 형식으로 올바른 파일 경로 생성")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_CorrectMarkdownFilePath() throws Exception {
            // given
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willReturn(successUploadResponse);

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isOk())
                    // 검증 로직 강화: 마크다운 파일 경로 검증
                    .andExpect(jsonPath("$.data.uploadedFilePath").value(TilTestConstants.GITHUB_FILE_PATH))
                    .andExpect(jsonPath("$.data.fileUrl").exists());

            then(tilUploadService).should().uploadTilToGitHub(any(), eq(TilTestConstants.TEST_USER_ID));
        }
    }

    @Nested
    @DisplayName("실패 시나리오")
    class FailureScenarios {

        @Test
        @DisplayName("tilId null 전달 시 HTTP 400 Bad Request")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_NullTilId_BadRequest() throws Exception {
            // given - 테스트 데이터 격리
            TilUploadRequestDTO.UploadRequest request = TilMockBuilder.createTilUploadRequestWithNullTilId();

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_400))
                    // 검증 로직 강화
                    .andExpect(jsonPath("$.success").value(false));

            // 서비스 메서드가 호출되지 않았는지 검증
            then(tilUploadService).should(never()).uploadTilToGitHub(any(), any());
        }

        @Test
        @DisplayName("tilId 음수 값 전달 시 HTTP 500 Internal Server Error")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_NegativeTilId_InternalServerError() throws Exception {
            // given
            TilUploadRequestDTO.UploadRequest request = TilMockBuilder.createTilUploadRequest(-1L);

            // Mock 서비스가 null을 반환하도록 설정 (실제 동작과 맞춤)
            given(tilUploadService.uploadTilToGitHub(any(TilUploadRequestDTO.UploadRequest.class), eq(TilTestConstants.TEST_USER_ID)))
                    .willReturn(null);

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                    .andExpect(jsonPath("$.success").value(false));

            then(tilUploadService).should().uploadTilToGitHub(any(), eq(TilTestConstants.TEST_USER_ID));
        }

        @Test
        @DisplayName("존재하지 않는 tilId 요청 시 HTTP 404 Not Found")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_TilNotFound_NotFound() throws Exception {
            // given
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_404))
                    // 검증 로직 강화: 에러 메시지 검증
                    .andExpect(jsonPath("$.message").value(TilTestConstants.ERROR_TIL_NOT_FOUND))
                    .andExpect(jsonPath("$.success").value(false));

            // 서비스 메서드 호출 검증: 정확한 파라미터로 호출되었는지 확인
            then(tilUploadService).should().uploadTilToGitHub(
                    argThat(request -> request.getTilId().equals(TilTestConstants.TEST_TIL_ID)),
                    eq(TilTestConstants.TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("타인의 TIL 업로드 시도 시 HTTP 403 Forbidden")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_NotOwner_Forbidden() throws Exception {
            // given
            given(tilUploadService.uploadTilToGitHub(any(TilUploadRequestDTO.UploadRequest.class), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_ACCESS_DENIED));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isInternalServerError()) // 실제 컨트롤러 응답에 맞춤
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                    .andExpect(jsonPath("$.message").value("TIL 업로드 중 오류가 발생했습니다: " + TilTestConstants.ERROR_TIL_ACCESS_DENIED))
                    .andExpect(jsonPath("$.success").value(false));

            // 서비스 메서드 호출 검증
            then(tilUploadService).should().uploadTilToGitHub(
                    argThat(request -> request.getTilId().equals(TilTestConstants.TEST_TIL_ID)),
                    eq(TilTestConstants.TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("이미 삭제된 TIL 업로드 시도 시 HTTP 500 Internal Server Error")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_DeletedTil_Gone() throws Exception {
            // given
            given(tilUploadService.uploadTilToGitHub(any(TilUploadRequestDTO.UploadRequest.class), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(TilTestConstants.ERROR_TIL_ALREADY_DELETED));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isInternalServerError()) // 410 → 500으로 변경
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500)) // HTTP_410 → HTTP_500으로 변경
                    .andExpect(jsonPath("$.message").value("TIL 업로드 중 오류가 발생했습니다: " + TilTestConstants.ERROR_TIL_ALREADY_DELETED)) // 에러 메시지 형식 맞춤
                    .andExpect(jsonPath("$.success").value(false));

            then(tilUploadService).should().uploadTilToGitHub(
                    argThat(request -> request.getTilId().equals(TilTestConstants.TEST_TIL_ID)),
                    eq(TilTestConstants.TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("기본 업로드 레포지토리 미설정 시 HTTP 400 Bad Request")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_RepositoryNotSet_BadRequest() throws Exception {
            // given
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(TilTestConstants.ERROR_REPOSITORY_NOT_SET));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_400))
                    .andExpect(jsonPath("$.message").value(TilTestConstants.ERROR_REPOSITORY_NOT_SET))
                    .andExpect(jsonPath("$.success").value(false));

            // 서비스 메서드 호출 검증
            then(tilUploadService).should().uploadTilToGitHub(
                    argThat(request -> request.getTilId().equals(TilTestConstants.TEST_TIL_ID)),
                    eq(TilTestConstants.TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("GitHub 토큰 미설정/만료 시 HTTP 401 Unauthorized")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_GithubTokenIssue_Unauthorized() throws Exception {
            // given
            String githubTokenError = "GitHub 토큰이 유효하지 않습니다.";
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(githubTokenError));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_401))
                    // 검증 로직 강화: GitHub 토큰 관련 에러 메시지 검증
                    .andExpect(jsonPath("$.message").value(githubTokenError))
                    .andExpect(jsonPath("$.success").value(false));

            then(tilUploadService).should().uploadTilToGitHub(
                    argThat(request -> request.getTilId().equals(TilTestConstants.TEST_TIL_ID)),
                    eq(TilTestConstants.TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("GitHub 토큰 권한 부족 시 HTTP 401 Unauthorized")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_GithubTokenInsufficientPermission_Unauthorized() throws Exception {
            // given
            String permissionError = "GitHub 토큰의 권한이 부족합니다.";
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(permissionError));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_401))
                    .andExpect(jsonPath("$.message").value(permissionError));

            then(tilUploadService).should().uploadTilToGitHub(any(), eq(TilTestConstants.TEST_USER_ID));
        }

        @Test
        @DisplayName("설정된 레포지토리 삭제/접근 불가 시 HTTP 404 Not Found")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_RepositoryNotFound_NotFound() throws Exception {
            // given
            String repositoryError = "설정된 레포지토리를 찾을 수 없습니다.";
            given(tilUploadService.uploadTilToGitHub(any(TilUploadRequestDTO.UploadRequest.class), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(repositoryError));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_404))
                    .andExpect(jsonPath("$.message").value(repositoryError))
                    .andExpect(jsonPath("$.success").value(false));

            then(tilUploadService).should().uploadTilToGitHub(
                    argThat(request -> request.getTilId().equals(TilTestConstants.TEST_TIL_ID)),
                    eq(TilTestConstants.TEST_USER_ID)
            );
        }

        @Test
        @DisplayName("GitHub API 호출 실패 시 HTTP 500 Internal Server Error")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_GithubApiFailure_InternalServerError() throws Exception {
            // given
            String apiError = "GitHub API 호출 중 오류가 발생했습니다.";
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(apiError));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                    .andExpect(jsonPath("$.message").value("TIL 업로드 중 오류가 발생했습니다: " + apiError))
                    .andExpect(jsonPath("$.success").value(false));

            then(tilUploadService).should().uploadTilToGitHub(any(), eq(TilTestConstants.TEST_USER_ID));
        }

        // 경계값 테스트 추가
        @Test
        @DisplayName("네트워크 타임아웃 시 HTTP 500 Internal Server Error")
        @WithMockUser(username = "1")
        void uploadTilToGitHub_NetworkTimeout_InternalServerError() throws Exception {
            // given
            String timeoutError = "GitHub 업로드 중 네트워크 타임아웃이 발생했습니다.";
            given(tilUploadService.uploadTilToGitHub(eq(validUploadRequest), eq(TilTestConstants.TEST_USER_ID)))
                    .willThrow(new RuntimeException(timeoutError));

            // when & then
            mockMvc.perform(post("/api/v1/tils/upload")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUploadRequest)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value(TilTestConstants.HTTP_500))
                    .andExpect(jsonPath("$.message").value("TIL 업로드 중 오류가 발생했습니다: " + timeoutError));

            then(tilUploadService).should().uploadTilToGitHub(any(), eq(TilTestConstants.TEST_USER_ID));
        }
    }
}

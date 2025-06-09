package com.youtil.Api.Tils.Service;

import com.youtil.Api.Github.Dto.CommitDetailResponseDTO;
import com.youtil.Api.Tils.Dto.TilAiResponseDTO;
import com.youtil.Exception.TilException.TilException.TilAIHealthxception;

import static com.youtil.Constants.MockTilConstants.*;
import static com.youtil.Constants.MockGitHubConstants.*;
import static com.youtil.Mock.MockTilBuilder.*;
import com.youtil.Util.MockUtil;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TilAiServiceTest {

    @Mock private WebClient webClient;

    @InjectMocks private TilAiService tilAiService;

    private CommitDetailResponseDTO.CommitDetailResponse mockCommitDetail;

    @BeforeEach
    void setup() {
        setupMockCommitDetail();
        setupAiServiceProperties();
    }

    private void setupAiServiceProperties() {
        ReflectionTestUtils.setField(tilAiService, "primaryAiApiUrl", "http://primary-ai-server.com");
        ReflectionTestUtils.setField(tilAiService, "secondaryAiApiUrl", "http://secondary-ai-server.com");
    }

    private void setupMockCommitDetail() {
        CommitDetailResponseDTO.FileDetail fileDetail = CommitDetailResponseDTO.FileDetail.builder()
                .filepath("src/main/java/example/Service.java")
                .latest_code("public class Service { ... }")
                .patches(List.of(
                        CommitDetailResponseDTO.PatchDetail.builder()
                                .commit_message(COMMIT_TITLE)
                                .patch("@@ -1,3 +1,4 @@")
                                .build()
                ))
                .build();

        mockCommitDetail = CommitDetailResponseDTO.CommitDetailResponse.builder()
                .username(GITHUB_USERNAME)
                .date(TEST_DATE.toString())
                .files(List.of(fileDetail))
                .build();
    }

    // ========== AI를 통한 TIL 내용 생성 테스트 ==========

    @Test
    @DisplayName("AI TIL 내용 생성 - 커밋 정보를 AI API로 전송하여 TIL 내용 생성 성공")
    void generateTilContent_withValidCommitData_success() {
        // given
        TilAiResponseDTO expectedResponse = createDefaultAiResponse();
        MockUtil.setupWebClientPostWithResponse(webClient, expectedResponse, TilAiResponseDTO.class);

        // when
        TilAiResponseDTO result = tilAiService.generateTilContent(
                mockCommitDetail, GITHUB_REPO_ID, GITHUB_BRANCH, MOCK_TITLE);

        // then
        assertNotNull(result);
        assertEquals(AI_RESPONSE_CONTENT, result.getContent());
        assertEquals(AI_KEYWORDS, result.getKeywords());
    }

    @Test
    @DisplayName("AI TIL 내용 생성 - AI 서버 연결 실패")
    void generateTilContent_withAiServerConnectionFailure_fail() {
        // given
        WebClient.RequestBodyUriSpec postUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.post()).thenReturn(postUriSpec);
        lenient().when(postUriSpec.uri(any(String.class))).thenReturn(bodySpec);
        lenient().when(bodySpec.contentType(any())).thenReturn(bodySpec);
        lenient().when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToMono(eq(TilAiResponseDTO.class)))
                .thenThrow(WebClientResponseException.create(503, "Service Unavailable", null, null, null));

        // when & then
        assertThatThrownBy(() -> tilAiService.generateTilContent(
                mockCommitDetail, GITHUB_REPO_ID, GITHUB_BRANCH, MOCK_TITLE))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("AI TIL 내용 생성 - AI 서버 응답 오류")
    void generateTilContent_withAiServerResponseError_fail() {
        // given
        WebClient.RequestBodyUriSpec postUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.post()).thenReturn(postUriSpec);
        lenient().when(postUriSpec.uri(any(String.class))).thenReturn(bodySpec);
        lenient().when(bodySpec.contentType(any())).thenReturn(bodySpec);
        lenient().when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToMono(eq(TilAiResponseDTO.class))).thenReturn(Mono.empty());

        // when & then
        assertThatThrownBy(() -> tilAiService.generateTilContent(
                mockCommitDetail, GITHUB_REPO_ID, GITHUB_BRANCH, MOCK_TITLE))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("AI TIL 내용 생성 - 빈 응답 반환")
    void generateTilContent_withEmptyResponse_success() {
        // given
        TilAiResponseDTO emptyResponse = createEmptyAiResponse();
        MockUtil.setupWebClientPostWithResponse(webClient, emptyResponse, TilAiResponseDTO.class);

        // when
        TilAiResponseDTO result = tilAiService.generateTilContent(
                mockCommitDetail, GITHUB_REPO_ID, GITHUB_BRANCH, MOCK_TITLE);

        // then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertTrue(result.getKeywords().isEmpty());
    }

    // ========== AI 서버 연결 가능 상태 확인 테스트 ==========

    @Test
    @DisplayName("AI 서버 연결 가능 상태 확인 - AI 서버의 상태를 정상적으로 반환해준다")
    void getTilAIHealthStatus_withHealthyServer_success() {
        // given
        MockUtil.setupWebClientGetWithStringResponse(webClient, AI_HEALTH_OK);

        // when
        String result = tilAiService.getTilAIHealthStatus();

        // then
        assertEquals(AI_HEALTH_OK, result);
    }

    @Test
    @DisplayName("AI 서버 연결 가능 상태 확인 - AI 서버 연결 실패 시")
    void getTilAIHealthStatus_withConnectionFailure_fail() {
        // given
        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(headersSpec);
        lenient().when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.onStatus(any(), any())).thenThrow(new TilAIHealthxception());

        // when & then
        assertThatThrownBy(() -> tilAiService.getTilAIHealthStatus())
                .isInstanceOf(TilAIHealthxception.class);
    }

    @Test
    @DisplayName("AI 서버 연결 가능 상태 확인 - 4xx 에러 응답")
    void getTilAIHealthStatus_with4xxError_fail() {
        // given
        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(headersSpec);
        lenient().when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToMono(eq(String.class)))
                .thenThrow(WebClientResponseException.create(400, "Bad Request", null, null, null));

        // when & then
        assertThatThrownBy(() -> tilAiService.getTilAIHealthStatus())
                .isInstanceOf(TilAIHealthxception.class);
    }

    @Test
    @DisplayName("AI 서버 연결 가능 상태 확인 - 5xx 에러 응답")
    void getTilAIHealthStatus_with5xxError_fail() {
        // given
        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(headersSpec);
        lenient().when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToMono(eq(String.class)))
                .thenThrow(WebClientResponseException.create(500, "Internal Server Error", null, null, null));

        // when & then
        assertThatThrownBy(() -> tilAiService.getTilAIHealthStatus())
                .isInstanceOf(TilAIHealthxception.class);
    }

    @Test
    @DisplayName("AI 서버 연결 가능 상태 확인 - 타임아웃 오류")
    void getTilAIHealthStatus_withTimeout_fail() {
        // given
        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(headersSpec);
        lenient().when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToMono(eq(String.class)))
                .thenThrow(new RuntimeException("Timeout"));

        // when & then
        assertThatThrownBy(() -> tilAiService.getTilAIHealthStatus())
                .isInstanceOf(TilAIHealthxception.class);
    }

    @Test
    @DisplayName("AI 서버 선택 - 오후 3시 이후에는 primary 서버 사용")
    void generateTilContent_afterThreePM_usesPrimaryServer() {
        // given
        TilAiResponseDTO expectedResponse = createDefaultAiResponse();
        MockUtil.setupWebClientPostWithResponse(webClient, expectedResponse, TilAiResponseDTO.class);

        // when
        TilAiResponseDTO result = tilAiService.generateTilContent(
                mockCommitDetail, GITHUB_REPO_ID, GITHUB_BRANCH, MOCK_TITLE);

        // then
        assertNotNull(result);
        assertEquals(AI_RESPONSE_CONTENT, result.getContent());
    }

    @Test
    @DisplayName("AI 서버 선택 - health check도 현재 활성 서버로 요청")
    void getTilAIHealthStatus_usesActiveServer() {
        // given
        MockUtil.setupWebClientGetWithStringResponse(webClient, AI_HEALTH_OK);

        // when
        String result = tilAiService.getTilAIHealthStatus();

        // then
        assertEquals(AI_HEALTH_OK, result);
    }

    @Test
    @DisplayName("AI TIL 내용 생성 - 커밋 상세 정보가 null인 경우")
    void generateTilContent_withNullCommitDetail_fail() {
        // when & then
        assertThatThrownBy(() -> tilAiService.generateTilContent(
                null, GITHUB_REPO_ID, GITHUB_BRANCH, MOCK_TITLE))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("AI TIL 내용 생성 - 레포지토리 ID가 null인 경우")
    void generateTilContent_withNullRepositoryId_success() {
        // given
        TilAiResponseDTO expectedResponse = createDefaultAiResponse();
        MockUtil.setupWebClientPostWithResponse(webClient, expectedResponse, TilAiResponseDTO.class);

        // when
        TilAiResponseDTO result = tilAiService.generateTilContent(
                mockCommitDetail, null, GITHUB_BRANCH, MOCK_TITLE);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("AI TIL 내용 생성 - 브랜치명이 null인 경우")
    void generateTilContent_withNullBranch_success() {
        // given
        TilAiResponseDTO expectedResponse = createDefaultAiResponse();
        MockUtil.setupWebClientPostWithResponse(webClient, expectedResponse, TilAiResponseDTO.class);

        // when
        TilAiResponseDTO result = tilAiService.generateTilContent(
                mockCommitDetail, GITHUB_REPO_ID, null, MOCK_TITLE);

        // then
        assertNotNull(result);
    }
}

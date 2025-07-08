package com.youtil.Util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.function.Function;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class MockUtil {

    // 기존 setupWebClientWithJsonNodeMock 메서드는 그대로 유지...
    public static void setupWebClientWithJsonNodeMock(WebClient webClient, JsonNode mockResponse) {
        WebClient.RequestHeadersUriSpec getUriSpec;
        WebClient.RequestHeadersSpec getHeaderSpec;
        WebClient.ResponseSpec getResponseSpec;

        getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(Function.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        lenient().when(getResponseSpec.bodyToMono(eq(JsonNode.class))).thenReturn(Mono.just(mockResponse));
    }

    /**
     * WebClient GET 요청 Mock 설정 (Map 응답용)
     */
    public static void setupWebClientGetWithMapResponse(WebClient webClient, Map<String, Object> mockResponse) {
        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(getHeaderSpec);
        lenient().when(getUriSpec.uri(any(Function.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.header(anyString(), anyString())).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        lenient().when(getResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(mockResponse));
    }

    /**
     * WebClient GET 요청 Mock 설정 (Map[] 응답용)
     */
    public static void setupWebClientGetWithMapArrayResponse(WebClient webClient, Map<String, Object>[] mockResponse) {
        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(getHeaderSpec);
        lenient().when(getUriSpec.uri(any(Function.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.header(anyString(), anyString())).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        lenient().when(getResponseSpec.bodyToMono(eq(Map[].class))).thenReturn(Mono.just(mockResponse));
    }

    /**
     * WebClient GET 요청 Mock 설정 (다중 응답 타입 지원 - Repository 메타데이터 + 브랜치 목록)
     */
    public static void setupWebClientGetWithMultipleResponses(WebClient webClient,
                                                              Map<String, Object> repositoryResponse,
                                                              Map<String, Object>[] branchesResponse) {

        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(getHeaderSpec);
        lenient().when(getUriSpec.uri(any(Function.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.header(anyString(), anyString())).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);

        // Repository 메타데이터 (Map.class)와 브랜치 목록 (Map[].class) 모두 지원
        lenient().when(getResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(repositoryResponse));
        lenient().when(getResponseSpec.bodyToMono(eq(Map[].class))).thenReturn(Mono.just(branchesResponse));
    }

    /**
     * WebClient POST 요청 Mock 설정
     */
    @SuppressWarnings("unchecked")
    public static void setupWebClientPostWithResponse(WebClient webClient, Object mockResponse, Class<?> responseType) {
        WebClient.RequestBodyUriSpec postUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.post()).thenReturn(postUriSpec);
        lenient().when(postUriSpec.uri(any(String.class))).thenReturn(bodySpec);
        lenient().when(bodySpec.contentType(any())).thenReturn(bodySpec);
        lenient().when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.bodyToMono(eq(responseType))).thenReturn((Mono) Mono.just(mockResponse));
        lenient().when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    /**
     * WebClient GET 요청 Mock 설정 (String 응답용) - Health Check 등에 사용
     */
    public static void setupWebClientGetWithStringResponse(WebClient webClient, String mockResponse) {
        WebClient.RequestHeadersUriSpec getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec getResponseSpec = mock(WebClient.ResponseSpec.class);

        lenient().when(webClient.get()).thenReturn(getUriSpec);
        lenient().when(getUriSpec.uri(any(String.class))).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.header(anyString(), anyString())).thenReturn(getHeaderSpec);
        lenient().when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        lenient().when(getResponseSpec.bodyToMono(eq(String.class))).thenReturn(Mono.just(mockResponse));
        lenient().when(getResponseSpec.onStatus(any(), any())).thenReturn(getResponseSpec);
    }
}

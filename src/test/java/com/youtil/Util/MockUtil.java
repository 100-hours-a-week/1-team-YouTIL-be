package com.youtil.Util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.Function;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component

public class MockUtil {


    //웹클라이언트 JsonNode모킹
    public static void setupWebClientWithJsonNodeMock(WebClient webClient, JsonNode mockResponse) {

        WebClient.RequestHeadersUriSpec getUriSpec;
        WebClient.RequestHeadersSpec getHeaderSpec;
        WebClient.ResponseSpec getResponseSpec;

        getUriSpec = mock(WebClient.RequestHeadersUriSpec.class);

        getHeaderSpec = mock(WebClient.RequestHeadersSpec.class);

        getResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get())
                .thenReturn(getUriSpec);

        when(getUriSpec.uri(any(Function.class))).thenReturn(getHeaderSpec);
        when(getHeaderSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(JsonNode.class))).thenReturn(Mono.just(mockResponse));
    }
}

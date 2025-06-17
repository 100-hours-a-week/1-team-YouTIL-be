package com.youtil.Api.Tils.Controller;


import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Sse.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
public class TilSseController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/subscribe/{requestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String requestId) {
        return sseEmitterService.getEmitter(requestId, AiType.TIL.toString());
    }
}

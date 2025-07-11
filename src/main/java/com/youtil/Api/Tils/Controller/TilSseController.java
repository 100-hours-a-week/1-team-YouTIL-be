package com.youtil.Api.Tils.Controller;


import com.youtil.Common.Enums.AiProgress;
import com.youtil.Common.Enums.AiType;
import com.youtil.Common.Sse.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1/tils")
@RequiredArgsConstructor
public class TilSseController {

    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/subscribe/{requestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String requestId) {
        log.info("SSE 연결 성공");
        return sseEmitterService.getEmitter(requestId, AiType.TIL.toString());
    }
    @GetMapping(value = "/subscribe/{requestId}/success",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter mockSseSuccess(@PathVariable String requestId) {
        {
            log.info("SSE 연결 성공");
            SseEmitter emitter = sseEmitterService.getEmitter(requestId, AiType.TIL.toString());

            new Thread(() -> {
                try {
                    sseEmitterService.send(requestId, AiProgress.WAITING, 2, 3);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.WAITING, 1, 2);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.PROCESSING, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.GET_COMMIT_DATA_FROM_GITHUB, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.COMMIT_ANALYSIS_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.SUPERVISOR_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.RESEARCH_TEAM_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.INTRODUCTION_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.CONCLUSION_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.FINISHED, 0, 0);
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            ).start();
            return emitter;
        }

    }
    @GetMapping(value = "/subscribe/{requestId}/fail",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter mockSseFail(@PathVariable String requestId) {
        {
            log.info("SSE 연결 성공");
            SseEmitter emitter = sseEmitterService.getEmitter(requestId, AiType.TIL.toString());

            new Thread(() -> {
                try {
                    sseEmitterService.send(requestId, AiProgress.WAITING, 2, 3);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.WAITING, 1, 2);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.PROCESSING, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.ERROR, 0, 0);
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            ).start();
            return emitter;
        }

    }
    @GetMapping(value = "/sse/mock/{requestId}/success",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter mockSuccess(@PathVariable String requestId) {
        {
            log.info("SSE 연결 성공");
            SseEmitter emitter = sseEmitterService.getEmitter(requestId, AiType.TIL.toString());

            new Thread(() -> {
                try {
                    sseEmitterService.send(requestId, AiProgress.WAITING, 2, 3);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.WAITING, 1, 2);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.PROCESSING, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.GET_COMMIT_DATA_FROM_GITHUB, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.COMMIT_ANALYSIS_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.SUPERVISOR_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.RESEARCH_TEAM_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.INTRODUCTION_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.CONCLUSION_START, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.FINISHED, 0, 0);
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            ).start();
            return emitter;
        }

    }
    @GetMapping(value = "/sse/mock/{requestId}/fail",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter mockFail(@PathVariable String requestId) {
        {
            log.info("SSE 연결 성공");
            SseEmitter emitter = sseEmitterService.getEmitter(requestId, AiType.TIL.toString());

            new Thread(() -> {
                try {
                    sseEmitterService.send(requestId, AiProgress.WAITING, 2, 3);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.WAITING, 1, 2);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.PROCESSING, 0, 0);
                    Thread.sleep(1000);
                    sseEmitterService.send(requestId, AiProgress.ERROR, 0, 0);
                    Thread.sleep(1000);

                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            ).start();
            return emitter;
        }

    }
}
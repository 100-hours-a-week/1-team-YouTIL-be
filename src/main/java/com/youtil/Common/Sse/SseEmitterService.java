package com.youtil.Common.Sse;

import com.youtil.Api.Tils.Dto.TilResponseDTO.TilStatus;
import com.youtil.Common.Enums.AiProgress;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterService {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final long RESEND_TIMEOUT = 5 * 60 * 1000L;

    public SseEmitter getEmitter(String requestId, String type) {
        SseEmitter emitter = new SseEmitter(RESEND_TIMEOUT);
        emitterMap.put(requestId, emitter);

        emitter.onTimeout(() -> emitterMap.remove(requestId));
        emitter.onCompletion(() -> emitterMap.remove(requestId));
        return emitter;
    }

    public void send(String requestId, Object data) {
        SseEmitter emitter = emitterMap.get(requestId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(data));

                if (isTerminalStatus(data)) {
                    emitter.complete();
                    emitterMap.remove(requestId);
                }

            } catch (IOException e) {
                emitterMap.remove(requestId);
            }
        }
    }

    private boolean isTerminalStatus(Object data) {
        if (data instanceof TilStatus tilStatus) {
            return AiProgress.FINISHED.toString().equals(tilStatus.getStatus())
                    || AiProgress.ERROR.toString().equals(
                    tilStatus.getStatus());
        }
        return true; // 기본적으로 안전하게 처리
    }
}

package com.youtil.Common.Sse;

import com.youtil.Api.Tils.Dto.TilResponseDTO.TilStatus;
import com.youtil.Common.Enums.AiProgress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterService {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final Map<String, List<Object>> statusCache = new ConcurrentHashMap<>();
    private final long RESEND_TIMEOUT = 5 * 60 * 1000L;

    public SseEmitter getEmitter(String requestId, String type) {
        SseEmitter emitter = new SseEmitter(RESEND_TIMEOUT);
        emitterMap.put(requestId, emitter);

        emitter.onTimeout(() -> emitterMap.remove(requestId));
        emitter.onCompletion(() -> emitterMap.remove(requestId));

        List<Object> cachedStatuses = statusCache.getOrDefault(requestId, List.of());
        for (Object status : cachedStatuses) {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(status));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
        return emitter;
    }

    public void send(String requestId, Object data) {
        internalSend(requestId, data, false);
    }

    public void send(String requestId, AiProgress aiProgress, int position, long total) {
        TilStatus status = TilStatus.builder()
                .requestId(requestId)
                .status(aiProgress.name())
                .position(position)
                .total(total)
                .build();
        internalSend(requestId, status, false);
    }

    private void internalSend(String requestId, Object data, boolean terminal) {
        SseEmitter emitter = emitterMap.get(requestId);
        statusCache.computeIfAbsent(requestId, key -> new ArrayList<>()).add(data);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(data));
                if (terminal || isTerminalStatus(data)) {
                    emitter.complete();
                    emitterMap.remove(requestId);
                    statusCache.remove(requestId);
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
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

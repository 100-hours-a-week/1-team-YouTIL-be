package com.youtil.Api.Tils.Dto;

import lombok.Getter;
import org.springframework.kafka.support.Acknowledgment;

@Getter
public class PrioritizedTilRequest implements Comparable<PrioritizedTilRequest> {

    private final String requestJson;
    private final Long userId;
    private final String requestId;
    private final long enqueueTime;
    private final Acknowledgment ack;

    public PrioritizedTilRequest(String requestJson, Long userId, String requestId,
            long enqueueTime, Acknowledgment ack) {
        this.requestJson = requestJson;
        this.userId = userId;
        this.requestId = requestId;
        this.enqueueTime = enqueueTime;
        this.ack = ack;
    }


    @Override
    public int compareTo(PrioritizedTilRequest o) {
        int cmp = Long.compare(this.enqueueTime, o.enqueueTime);
        if (cmp != 0) {
            return cmp;
        }
        return this.requestId.compareTo(o.requestId);
    }
}

package com.youtil.Api.Filtering.Dto;

import com.youtil.Common.Dto.QueueRequest;
import lombok.Getter;
import org.springframework.kafka.support.Acknowledgment;

@Getter
public class PrioritizedFilterReqeust implements Comparable<PrioritizedFilterReqeust>,
        QueueRequest {


    private final String requestJson;
    private final Long userId;
    private final String requestId;
    private final long enqueueTime;
    private final Acknowledgment ack;

    public PrioritizedFilterReqeust(String requestJson, Long userId, String requestId,
            long enqueueTime, Acknowledgment ack) {
        this.requestJson = requestJson;
        this.userId = userId;
        this.requestId = requestId;
        this.enqueueTime = enqueueTime;
        this.ack = ack;
    }


    @Override
    public int compareTo(PrioritizedFilterReqeust o) {
        int cmp = Long.compare(this.enqueueTime, o.enqueueTime);
        if (cmp != 0) {
            return cmp;
        }
        return this.requestId.compareTo(o.requestId);
    }

}

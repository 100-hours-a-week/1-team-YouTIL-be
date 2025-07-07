package com.youtil.Api.Interview.dto;

import com.youtil.Common.Dto.QueueRequest;
import lombok.Getter;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.kafka.support.Acknowledgment;

@Getter
public class PrioritizedInterviewRequest implements Comparable<PrioritizedInterviewRequest>,
        QueueRequest {

    private final String requestJson;
    private final Long userId;
    private final String requestId;
    private final long enqueueTime;
    private final Acknowledgment ack;

    public PrioritizedInterviewRequest(String requestJson, Long userId, String requestId,
            long enqueueTime, Acknowledgment ack) {
        this.requestJson = requestJson;
        this.userId = userId;
        this.requestId = requestId;
        this.enqueueTime = enqueueTime;
        this.ack = ack;
    }

    private static long extractTimestampFromStreamId(MapRecord<String, Object, Object> record) {
        String streamId = record.getId().getValue();
        return Long.parseLong(streamId.split("-")[0]);
    }

    @Override
    public int compareTo(PrioritizedInterviewRequest o) {
        int cmp = Long.compare(this.enqueueTime, o.enqueueTime);
        if (cmp != 0) {
            return cmp;
        }
        return this.requestId.compareTo(o.requestId);
    }
}

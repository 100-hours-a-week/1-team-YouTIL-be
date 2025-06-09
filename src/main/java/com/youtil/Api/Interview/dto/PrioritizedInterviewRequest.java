package com.youtil.Api.Interview.dto;

import lombok.Getter;
import org.springframework.data.redis.connection.stream.MapRecord;

public class PrioritizedInterviewRequest implements Comparable<PrioritizedInterviewRequest> {

    private final long streamTimestamp;
    @Getter
    private final MapRecord<String, Object, Object> record;

    public PrioritizedInterviewRequest(MapRecord<String, Object, Object> record) {
        this.record = record;
        this.streamTimestamp = extractTimestampFromStreamId(record);
    }

    private static long extractTimestampFromStreamId(MapRecord<String, Object, Object> record) {
        String streamId = record.getId().getValue();
        return Long.parseLong(streamId.split("-")[0]);
    }

    @Override
    public int compareTo(PrioritizedInterviewRequest o) {
        return Long.compare(this.streamTimestamp, o.streamTimestamp);
    }
}

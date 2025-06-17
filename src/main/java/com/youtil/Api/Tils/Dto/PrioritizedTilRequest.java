package com.youtil.Api.Tils.Dto;

import lombok.Getter;
import org.springframework.data.redis.connection.stream.MapRecord;

public class PrioritizedTilRequest implements Comparable<PrioritizedTilRequest> {

    private final long streamTimestamp;
    @Getter
    private final MapRecord<String, Object, Object> record;
    @Getter
    private final String requestId;

    public PrioritizedTilRequest(MapRecord<String, Object, Object> record) {
        this.record = record;
        this.streamTimestamp = extractTimestampFromStreamId(record);
        this.requestId = (String) record.getValue().get("request_id");
    }

    private static long extractTimestampFromStreamId(MapRecord<String, Object, Object> record) {
        String streamId = record.getId().getValue();
        return Long.parseLong(streamId.split("-")[0]);
    }

    @Override
    public int compareTo(PrioritizedTilRequest o) {
        return Long.compare(this.streamTimestamp, o.streamTimestamp);
    }
}

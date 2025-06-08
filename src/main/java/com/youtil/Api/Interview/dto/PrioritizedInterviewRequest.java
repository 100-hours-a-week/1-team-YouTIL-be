package com.youtil.Api.Interview.dto;

import lombok.Getter;
import org.springframework.data.redis.connection.stream.MapRecord;

public class PrioritizedInterviewRequest  implements Comparable<PrioritizedInterviewRequest>{

    private final long createdAt;
    @Getter
    private final MapRecord<String, Object, Object> record;

    public PrioritizedInterviewRequest(MapRecord<String, Object, Object> record) {
        this.record = record;
        this.createdAt = System.currentTimeMillis();
    }

    @Override
    public int compareTo(PrioritizedInterviewRequest o) {
        return Long.compare(this.createdAt, o.createdAt); // 오래된 것이 먼저
    }
}

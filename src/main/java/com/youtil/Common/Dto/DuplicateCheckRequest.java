package com.youtil.Common.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckRequest {
    private String requestId;       // 고유 요청 ID
    private String action;          // 도메인
    private Long userId;            // 사용자 ID
    private String dataHash;        // 요청 데이터 해시값
    private long timestamp;         // 요청 시간
    private Object originalData;    // 원본 요청 데이터 (JSON)
}

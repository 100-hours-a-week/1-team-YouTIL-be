package com.youtil.Common.Dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KafkaMessageDto {

    private String type;
    private String payload;
    private String timestamp;
}

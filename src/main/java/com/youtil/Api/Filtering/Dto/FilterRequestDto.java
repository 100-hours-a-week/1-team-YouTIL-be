package com.youtil.Api.Filtering.Dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FilterRequestDto {

    private Long id;
    private String content;
    private String type;
}

package com.youtil.Exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class ExceptionResponse {
    private final boolean isSuccess = false;
    private String code;
    private String message;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private OffsetDateTime responseAt = OffsetDateTime.now();
}

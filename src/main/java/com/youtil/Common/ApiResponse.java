package com.youtil.Common;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private String message;
    private boolean success = true;
    private String code;
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private OffsetDateTime responseAt = OffsetDateTime.now();
    private T data = null;

    //Post 또는 GET
    public ApiResponse(String message, String code, T data) {
        this.message = message;
        this.code = code;
        this.data = data;
    }

    //UPDATE 또는 DELETE
    public ApiResponse(String message, String code) {
        this.message = message;
        this.code = code;
    }
}

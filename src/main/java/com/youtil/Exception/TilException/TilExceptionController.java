package com.youtil.Exception.TilException;

import com.youtil.Common.Enums.ErrorMessageCode;
import com.youtil.Exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TilExceptionController {

    @ExceptionHandler(TilException.TilAIHealthxception.class)
    public ResponseEntity<ExceptionResponse> TilAIUnHealthException(
            TilException.TilAIHealthxception e) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.AI_SEVER_NOT_HEALTH.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

}

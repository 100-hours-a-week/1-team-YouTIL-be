package com.youtil.Exception;

import com.youtil.Common.Enums.ErrorMessageCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionController {

    @ExceptionHandler(CommonException.ResourceNotFoundException.class)
    public ResponseEntity<ExceptionResponse> resourceNotFoundException(
            CommonException.ResourceNotFoundException e) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.RESOURCE_NOT_FOUND.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}

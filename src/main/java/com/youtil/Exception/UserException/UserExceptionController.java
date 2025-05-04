package com.youtil.Exception.UserException;

import com.youtil.Exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserExceptionController {

    @ExceptionHandler(UserException.UserNotFoundException.class)
    public ResponseEntity<ExceptionResponse> userNotFoundException(
            UserException.UserNotFoundException e) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode("400");
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserException.GitHubProfileNotFoundException.class)
    public ResponseEntity<ExceptionResponse> gitHubProfileNotFoundException(
            UserException.GitHubProfileNotFoundException e){
        ExceptionResponse response = new ExceptionResponse();
        response.setCode("400");
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}

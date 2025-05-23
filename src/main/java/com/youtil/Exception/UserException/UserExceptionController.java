package com.youtil.Exception.UserException;

import com.youtil.Common.Enums.ErrorMessageCode;
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
        response.setCode(ErrorMessageCode.USER_NOT_FOUND.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserException.GitHubProfileNotFoundException.class)
    public ResponseEntity<ExceptionResponse> gitHubProfileNotFoundException(
            UserException.GitHubProfileNotFoundException e) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.GITHUB_PROFILE_NOT_FOUND.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserException.WrongAuthorizationCodeException.class)
    public ResponseEntity<ExceptionResponse> wrongAuthorizationCodeException(
            UserException.WrongAuthorizationCodeException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.WRONG_AUTHORIZATION_CODE.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

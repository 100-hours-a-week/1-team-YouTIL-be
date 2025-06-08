package com.youtil.Exception.InterviewException;

import com.youtil.Common.Enums.ErrorMessageCode;
import com.youtil.Exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class InterviewExceptionController {

    @ExceptionHandler(InterviewException.InterviewNotFoundException.class)
    public ResponseEntity<ExceptionResponse> InterViewNotFoundException(
            InterviewException.InterviewNotFoundException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.INTERVIEW_NOT_FOUND.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InterviewException.InterviewNotMatchException.class)
    public ResponseEntity<ExceptionResponse> InterviewNotMatchException(
            InterviewException.InterviewNotMatchException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.NOT_MATCH_INTERVIEW.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}

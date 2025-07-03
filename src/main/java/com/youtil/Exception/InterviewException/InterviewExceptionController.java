package com.youtil.Exception.InterviewException;

import com.youtil.Common.Enums.ErrorMessageCode;
import com.youtil.Exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
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

    @ExceptionHandler(InterviewException.InterviewSerializationException.class)
    public ResponseEntity<ExceptionResponse> InterviewSerializationException(
            InterviewException.InterviewSerializationException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.INTERVIEW_QUEUE_SERIALIZATION_FAILED.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InterviewException.InterviewCreateTimeoutException.class)
    public ResponseEntity<ExceptionResponse> InterviewCreateTimeoutException(
            InterviewException.InterviewCreateTimeoutException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.INTERVIEW_CREATED_TIMEOUT.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.GATEWAY_TIMEOUT);
    }
}

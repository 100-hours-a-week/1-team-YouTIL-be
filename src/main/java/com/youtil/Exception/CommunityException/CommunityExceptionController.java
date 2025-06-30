package com.youtil.Exception.CommunityException;

import com.youtil.Common.Enums.ErrorMessageCode;
import com.youtil.Exception.CommunityException.CommunityException.CommentContentNotFoundException;
import com.youtil.Exception.CommunityException.CommunityException.CommentNotFoundException;
import com.youtil.Exception.ExceptionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class CommunityExceptionController {

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ExceptionResponse> commentNotFoundException(
            CommentNotFoundException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.COMMENT_NOT_FOUND.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CommentContentNotFoundException.class)
    public ResponseEntity<ExceptionResponse> commentContentNotFoundException(
            CommentContentNotFoundException e
    ) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode(ErrorMessageCode.COMMENT_CONTENT_NOT_FOUND.getCode());
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}

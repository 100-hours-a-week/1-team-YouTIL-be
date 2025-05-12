package com.youtil.Exception.StorageException;

import com.youtil.Exception.ExceptionResponse;
import com.youtil.Exception.UserException.UserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class StorageExceptionController {
    @ExceptionHandler(StorageException.ImageUploadException.class)
    public ResponseEntity<ExceptionResponse> imageUploadException(
            StorageException.ImageUploadException e) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode("400");
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StorageException.NotImageException.class)
    public ResponseEntity<ExceptionResponse> notImageException(
            StorageException.NotImageException e) {
        ExceptionResponse response = new ExceptionResponse();
        response.setCode("400");
        response.setMessage(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}

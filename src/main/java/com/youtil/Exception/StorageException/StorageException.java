package com.youtil.Exception.StorageException;

import com.youtil.Common.Enums.ErrorMessageCode;

public class StorageException {
    public static class ImageUploadException extends RuntimeException {

        public ImageUploadException() {
            super(ErrorMessageCode.IMAGE_UPLOAD_FAILED.getMessage());
        }

    }
    public static class NotImageException extends RuntimeException {
        public NotImageException() {
            super(ErrorMessageCode.NOT_MATCH_IMAGE.getMessage());
        }
    }
}

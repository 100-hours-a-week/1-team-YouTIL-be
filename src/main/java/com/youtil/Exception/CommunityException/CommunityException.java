package com.youtil.Exception.CommunityException;

import com.youtil.Common.Enums.ErrorMessageCode;

public class CommunityException extends RuntimeException {

    public static class CommentNotFoundException extends RuntimeException {

        public CommentNotFoundException() {
            super(ErrorMessageCode.INTERVIEW_NOT_FOUND.getMessage());
        }
    }

    public static class CommentContentNotFoundException extends RuntimeException {

        public CommentContentNotFoundException() {
            super(ErrorMessageCode.COMMENT_CONTENT_NOT_FOUND.getMessage());
        }
    }
}

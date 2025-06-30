package com.youtil.Exception.CommunityException;

import com.youtil.Common.Enums.ErrorMessageCode;

public class CommunityException extends RuntimeException {

    public static class CommentNotFoundException extends RuntimeException {

        public CommentNotFoundException() {
            super(ErrorMessageCode.COMMENT_NOT_FOUND.getMessage());
        }
    }

    public static class CommentContentNotFoundException extends RuntimeException {

        public CommentContentNotFoundException() {
            super(ErrorMessageCode.COMMENT_CONTENT_NOT_FOUND.getMessage());
        }
    }

    public static class CommentNotMatchedUserException extends RuntimeException {

        public CommentNotMatchedUserException() {
            super(ErrorMessageCode.COMMENT_NOT_MATCHED_OWNER.getMessage());
        }
    }

    public static class CommentNotMatchedTilException extends RuntimeException {

        public CommentNotMatchedTilException() {
            super(ErrorMessageCode.COMMENT_NOT_MATCHED_TIL.getMessage());
        }
    }
}

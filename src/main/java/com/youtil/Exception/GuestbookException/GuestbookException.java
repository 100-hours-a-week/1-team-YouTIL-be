package com.youtil.Exception.GuestbookException;

import com.youtil.Common.Enums.GuestbookMessageCode;

public class GuestbookException extends RuntimeException {

    private final GuestbookMessageCode messageCode;

    public GuestbookException(GuestbookMessageCode messageCode) {
        super(messageCode.getMessage());
        this.messageCode = messageCode;
    }

    public GuestbookException(String message) {
        super(message);
        this.messageCode = null;
    }

    public GuestbookMessageCode getMessageCode() {
        return messageCode;
    }

    public static class GuestbookNotFoundException extends GuestbookException {
        public GuestbookNotFoundException() {
            super(GuestbookMessageCode.GUESTBOOK_NOT_FOUND);
        }
    }

    public static class InvalidGuestbookAccessException extends GuestbookException {
        public InvalidGuestbookAccessException() {
            super(GuestbookMessageCode.INVALID_GUESTBOOK_ACCESS);
        }
    }

    public static class InvalidGuestbookContentException extends GuestbookException {
        public InvalidGuestbookContentException() {
            super(GuestbookMessageCode.INVALID_GUESTBOOK_CONTENT);
        }
    }

    public static class GuestbookContentTooLongException extends GuestbookException {
        public GuestbookContentTooLongException() {
            super(GuestbookMessageCode.GUESTBOOK_CONTENT_TOO_LONG);
        }
    }

    public static class GuestbookContentEmptyException extends GuestbookException {
        public GuestbookContentEmptyException() {
            super(GuestbookMessageCode.GUESTBOOK_CONTENT_EMPTY);
        }
    }

    public static class InvalidParentGuestbookException extends GuestbookException {
        public InvalidParentGuestbookException() {
            super(GuestbookMessageCode.INVALID_PARENT_GUESTBOOK);
        }
    }

    public static class GuestbookReplyDepthExceededException extends GuestbookException {
        public GuestbookReplyDepthExceededException() {
            super(GuestbookMessageCode.GUESTBOOK_REPLY_DEPTH_EXCEEDED);
        }
    }

    public static class CannotReplyToDeletedGuestbookException extends GuestbookException {
        public CannotReplyToDeletedGuestbookException() {
            super(GuestbookMessageCode.CANNOT_REPLY_TO_DELETED_GUESTBOOK);
        }
    }
}

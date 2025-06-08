package com.youtil.Exception.InterviewException;

import com.youtil.Common.Enums.ErrorMessageCode;

public class InterviewException {

    public static class InterviewNotFoundException extends RuntimeException {

        public InterviewNotFoundException() {
            super(ErrorMessageCode.INTERVIEW_NOT_FOUND.getMessage());
        }
    }

    public static class InterviewNotMatchException extends RuntimeException {

        public InterviewNotMatchException() {
            super(ErrorMessageCode.NOT_MATCH_INTERVIEW.getMessage());
        }
    }

    public static class InterviewSerializationException extends RuntimeException {
        public InterviewSerializationException() { super(ErrorMessageCode.INTERVIEW_QUEUE_SERIALIZATION_FAILED.getMessage());}
    }

    public static class InterviewCreateTimeoutException extends RuntimeException {
        public InterviewCreateTimeoutException() {
            super(ErrorMessageCode.INTERVIEW_CREATED_TIMEOUT.getMessage());
        }
    }

}

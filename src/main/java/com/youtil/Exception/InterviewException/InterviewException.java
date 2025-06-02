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

}

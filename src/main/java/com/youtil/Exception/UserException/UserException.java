package com.youtil.Exception.UserException;

import com.youtil.Common.Enums.ErrorMessageCode;

public class UserException {


    public static class UserNotFoundException extends RuntimeException {

        public UserNotFoundException() {
            super(ErrorMessageCode.USER_NOT_FOUND.getMessage());
        }

    }
     public static class GitHubProfileNotFoundException extends RuntimeException {
        public GitHubProfileNotFoundException() {
            super(ErrorMessageCode.GITHUB_PROFILE_NOT_FOUND.getMessage());
        }
    }

    public static class GitHubEmailNotFoundException extends RuntimeException {
        public GitHubEmailNotFoundException() {
            super(ErrorMessageCode.GITHUB_EMAIL_NOT_FOUND.getMessage());
        }
    }
}

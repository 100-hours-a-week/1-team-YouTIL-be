package com.youtil.Exception.GithubException;

import lombok.Getter;

/**
 * GitHub 관련 예외 클래스들
 */
public class GitHubExceptions {

    @Getter
    public static class GitHubException extends RuntimeException {
        private final String errorCode;
        private final int httpStatus;

        public GitHubException(String message, String errorCode, int httpStatus) {
            super(message);
            this.errorCode = errorCode;
            this.httpStatus = httpStatus;
        }

        public GitHubException(String message, String errorCode, int httpStatus, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
            this.httpStatus = httpStatus;
        }
    }

    public static class GitHubApiException extends GitHubException {
        public GitHubApiException(String message, int httpStatus) {
            super(message, "GITHUB_API_ERROR", httpStatus);
        }

        public GitHubApiException(String message, int httpStatus, Throwable cause) {
            super(message, "GITHUB_API_ERROR", httpStatus, cause);
        }
    }

    public static class GitHubTokenException extends GitHubException {
        public GitHubTokenException(String message) {
            super(message, "GITHUB_TOKEN_ERROR", 401);
        }

        public GitHubTokenException(String message, Throwable cause) {
            super(message, "GITHUB_TOKEN_ERROR", 401, cause);
        }
    }

    public static class GitHubValidationException extends GitHubException {
        public GitHubValidationException(String message) {
            super(message, "GITHUB_VALIDATION_ERROR", 400);
        }
    }
}

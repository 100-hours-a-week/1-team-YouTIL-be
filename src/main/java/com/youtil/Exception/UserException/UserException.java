package com.youtil.Exception.UserException;

import com.youtil.Common.Enums.ErrorMessageCode;

public class UserException {


    public static class UserNotFoundException extends RuntimeException {

        public UserNotFoundException() {
            super(ErrorMessageCode.USER_NOT_FOUND.getMessage());
        }
    }
}

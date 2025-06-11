package com.youtil.Exception;

import com.youtil.Common.Enums.ErrorMessageCode;

public class CommonException {

    public static class ResourceNotFoundException extends RuntimeException {

        public ResourceNotFoundException() {
            super(ErrorMessageCode.RESOURCE_NOT_FOUND.getMessage());
        }

    }

}

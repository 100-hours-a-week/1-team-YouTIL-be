package com.youtil.Exception.TilException;

import com.youtil.Common.Enums.ErrorMessageCode;

public class TilException {

    public static class TilAIHealthxception extends RuntimeException {

        public TilAIHealthxception() {
            super(ErrorMessageCode.AI_SEVER_NOT_HEALTH.getMessage());
        }

    }


    public static class TilSerializationException extends RuntimeException {

        public TilSerializationException() {
            super(ErrorMessageCode.TIL_QUEUE_SERIALIZATION_FAILED.getMessage());
        }
    }

    public static class TilCreateTimeOutException extends RuntimeException {

        public TilCreateTimeOutException() {
            super(ErrorMessageCode.TIL_CREATED_TIMEOUT.getMessage());
        }
    }

    public static class TilNotFoundException extends RuntimeException {

        public TilNotFoundException() {
            super(ErrorMessageCode.TIL_NOT_FOUND.getMessage());
        }
    }


}

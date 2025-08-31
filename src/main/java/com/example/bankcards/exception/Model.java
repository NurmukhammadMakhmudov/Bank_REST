package com.example.bankcards.exception;

public class Model extends RuntimeException {
    public Model(String message) {
        super(message);
    }

    public Model(String message, Throwable cause) {
        super(message, cause);
    }

    public Model(Throwable cause) {
        super(cause);
    }

    public Model(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

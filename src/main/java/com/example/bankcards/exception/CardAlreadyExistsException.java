package com.example.bankcards.exception;

public class CardAlreadyExistsException extends BusinessException{
    public CardAlreadyExistsException(String message) {
        super(message);
    }

    public CardAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public CardAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public CardAlreadyExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

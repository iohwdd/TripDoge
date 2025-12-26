package com.tripdog.exception;

public class LimitException extends RuntimeException {
    public LimitException(String message) {
        super(message);
    }
}

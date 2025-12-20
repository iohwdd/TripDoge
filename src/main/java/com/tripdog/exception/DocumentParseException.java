package com.tripdog.exception;

public class DocumentParseException extends RuntimeException {
    public DocumentParseException(String message) {
        super(message);
    }
    public DocumentParseException(Throwable cause) {
        super(cause);
    }
}

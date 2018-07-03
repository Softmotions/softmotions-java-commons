package com.softmotions.commons.mime;

public class MimeException extends RuntimeException {

    private static final long serialVersionUID = -1931354615779382666L;

    public MimeException(String message) {
        super(message);
    }

    public MimeException(Throwable t) {
        super(t);
    }

    public MimeException(String message, Throwable t) {
        super(message, t);
    }
}
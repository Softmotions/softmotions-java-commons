package com.softmotions.weboot.jaxrs;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class MessageErrorException extends MessageException {

    public MessageErrorException() {
        this("");
    }

    public MessageErrorException(String message) {
        super(message, true);
    }

    public MessageErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageErrorException(Throwable cause) {
        super(cause);
    }
}

package com.softmotions.commons.mongo;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class ConstraintsException extends RuntimeException {

    public ConstraintsException(String message) {
        super(message);
    }

    public ConstraintsException(String message, Throwable cause) {
        super(message, cause);
    }
}

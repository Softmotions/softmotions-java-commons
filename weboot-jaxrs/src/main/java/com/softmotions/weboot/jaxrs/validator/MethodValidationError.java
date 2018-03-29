package com.softmotions.weboot.jaxrs.validator;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class MethodValidationError implements Serializable {

    private final String key;

    private final String validator;

    private final String message;

    public String getKey() {
        return key;
    }

    public String getValidator() {
        return validator;
    }

    public String getMessage() {
        return message;
    }

    public MethodValidationError(String key, String message, String validator) {
        this.key = key;
        this.message = message;
        this.validator = validator;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("key", key)
                          .add("message", message)
                          .add("validator", validator)
                          .toString();
    }
}

package com.softmotions.weboot.jaxrs.validator;

import java.io.Serializable;

import com.google.common.base.MoreObjects;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class JaxrsMethodValidationError implements Serializable {

    private final String key;

    private final String validator;

    public String getKey() {
        return key;
    }

    public String getValidator() {
        return validator;
    }

    public JaxrsMethodValidationError(String key, String validator) {
        this.key = key;
        this.validator = validator;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("key", key)
                          .add("validator", validator)
                          .toString();
    }
}

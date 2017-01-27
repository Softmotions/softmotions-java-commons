package com.softmotions.weboot.jaxrs.validator;

import java.util.List;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class JaxrsMethodValidationException extends RuntimeException {

    final List<JaxrsMethodValidationError> errors;

    public List<JaxrsMethodValidationError> getErrors() {
        return errors;
    }

    public JaxrsMethodValidationException(List<JaxrsMethodValidationError> errors) {
        this.errors = errors;
    }
}
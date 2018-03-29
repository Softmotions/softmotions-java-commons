package com.softmotions.weboot.jaxrs.validator;

import java.util.List;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class MethodValidationException extends RuntimeException {

    final List<MethodValidationError> errors;

    public List<MethodValidationError> getErrors() {
        return errors;
    }

    public MethodValidationException(List<MethodValidationError> errors) {
        super(errors.isEmpty() ? "Validation error" : errors.get(0).toString());
        this.errors = errors;
    }


}
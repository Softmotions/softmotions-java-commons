package com.softmotions.weboot.jaxrs.validator;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class MethodValidationException extends RuntimeException {

    final Method method;

    final List<MethodValidationError> errors;

    public List<MethodValidationError> getErrors() {
        return errors;
    }

    public Method getMethod() {
        return method;
    }

    public MethodValidationException(Method method, MethodValidationError error) {
        super(error.toString());
        this.method = method;
        this.errors = Collections.singletonList(error);
    }

    public MethodValidationException(Method method, List<MethodValidationError> errors) {
        super(errors.isEmpty() ? "Validation error" : errors.get(0).toString());
        this.method = method;
        this.errors = errors;
    }


}
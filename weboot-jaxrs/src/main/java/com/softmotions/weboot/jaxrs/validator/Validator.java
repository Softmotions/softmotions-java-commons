package com.softmotions.weboot.jaxrs.validator;

import javax.annotation.Nullable;

/**
 * Validator for {@link JaxrsMethodValidator}
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface Validator {

    boolean validate(@Nullable Object value, String... args);
}

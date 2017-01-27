package com.softmotions.weboot.jaxrs.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Define validator group.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ValidatorsGroup {

    /**
     * Validator group name
     */
    String name();

    /**
     * Validator groups to be included
     */
    String[] includeGroups() default {};

    /**
     * Validation rules
     */
    String[] validators();
}

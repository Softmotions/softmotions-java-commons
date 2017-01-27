package com.softmotions.weboot.jaxrs.validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ValidateJsonBody {

    /**
     * Input JSON validation spec. todo docs
     */
    String[] validators() default {};

    /**
     * Validator groups to be included
     */
    String[] includeGroups() default {};
}

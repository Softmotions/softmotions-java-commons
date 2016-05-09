package com.softmotions.commons;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Human description of any class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(value = {ElementType.METHOD, ElementType.TYPE})
public @interface Human {
    /**
     * Description.
     */
    String value();
}

package com.softmotions.weboot.jaxrs.ws;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks method as WS action to handle a message. Messages are expected in JSON format
 * {"key":<keyname>, <data>}, with additional data passed within "query" object.
 * Method
 *
 * @author Vyacheslav Tyutyunkov (tve@softmotions.com)
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface WSAction {

    String value();

    String key() default "";
}


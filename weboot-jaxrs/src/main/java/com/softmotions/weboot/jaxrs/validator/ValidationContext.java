package com.softmotions.weboot.jaxrs.validator;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface ValidationContext {

    /**
     * Return request JSON body passed as argument of REST method.
     */
    @Nullable
    JsonNode getJsonBody();

    /**
     * List of validated values.
     * Validation key => value
     */
    @Nonnull
    Map<String, Object> getValidatedValues();
}

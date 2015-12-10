package com.softmotions.weboot.templates;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TemplateServiceException extends RuntimeException {

    public TemplateServiceException(String message) {
        super(message);
    }

    public TemplateServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

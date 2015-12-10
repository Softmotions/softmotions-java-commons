package com.softmotions.weboot.templates;

import java.util.Locale;

/**
 * Repository for string templates which will be
 * used in email and phone messages.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface TemplateService {

    TemplateContext get(String template, Locale locale) throws TemplateServiceException;

}

package com.softmotions.weboot.templates;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface TemplateContext {

    TemplateContext put(String key, Object val);

    Object get(String key);

    Object remove(String key);

    String[] keys();

    String render() throws TemplateServiceException;
}

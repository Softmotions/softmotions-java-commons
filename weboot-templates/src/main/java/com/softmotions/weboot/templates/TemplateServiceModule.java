package com.softmotions.weboot.templates;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TemplateServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TemplateService.class).to(TemplateServiceImpl.class).in(Singleton.class);
    }
}

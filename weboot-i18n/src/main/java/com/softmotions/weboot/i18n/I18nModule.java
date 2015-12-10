package com.softmotions.weboot.i18n;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class I18nModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(I18n.class).in(Singleton.class);
    }
}

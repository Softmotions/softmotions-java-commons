package com.softmotions.weboot.i18n;

import org.apache.commons.lang3.ArrayUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.softmotions.commons.ServicesConfiguration;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class I18nModule extends AbstractModule {

    private final ServicesConfiguration cfg;

    private final String[] bundles;

    public I18nModule() {
        this.cfg = null;
        this.bundles = ArrayUtils.EMPTY_STRING_ARRAY;
    }

    public I18nModule(ServicesConfiguration cfg, String... bundles) {
        this.cfg = cfg;
        this.bundles = bundles;
    }

    @Override
    protected void configure() {
        if (this.cfg == null) {
            bind(I18n.class).in(Singleton.class);
        } else {
            bind(I18n.class).toInstance(new I18n(this.cfg.xcfg(), bundles));
        }
    }
}

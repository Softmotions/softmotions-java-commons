package com.softmotions.weboot.jaxrs;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.softmotions.commons.ServicesConfiguration;

/**
 * Weboot JAX-RS module.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBJaxrsModule extends AbstractModule {

    private final ServicesConfiguration cfg;

    public WBJaxrsModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        bind(JsonNodeReader.class).in(Singleton.class);
        bind(JacksonContextResolver.class).in(Singleton.class);
        bind(ResteasyUTF8CharsetFilter.class).in(Singleton.class);

        // todo review it
        String appId = cfg.xcfg().getString("messages.appId", "");
        if (StringUtils.isBlank(appId)) {
            appId = cfg.xcfg().getString("app-name", "App");
        }
        MessageException.APP_ID = appId;
    }

    @Singleton
    @Provides
    public ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}

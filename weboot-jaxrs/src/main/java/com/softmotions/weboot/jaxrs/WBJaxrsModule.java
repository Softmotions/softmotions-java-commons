package com.softmotions.weboot.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Weboot JAX-RS module.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBJaxrsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(JsonNodeReader.class).in(Singleton.class);
        bind(JacksonContextResolver.class).in(Singleton.class);
        bind(ResteasyUTF8CharsetFilter.class).in(Singleton.class);
    }

    @Singleton
    @Provides
    public ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}

package com.softmotions.weboot.jaxrs;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(WBJaxrsModule.class);

    private final ServicesConfiguration cfg;

    /**
     * For backward compatibility with dependent sowtware
     */
    @Deprecated
    public WBJaxrsModule() {
        log.warn("!!!! Used deprecated WBJaxrsModule constructor, please upgrade your code !!!");
        this.cfg = null;
    }

    public WBJaxrsModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        bind(JsonNodeReader.class).in(Singleton.class);
        bind(JacksonContextResolver.class).in(Singleton.class);
        bind(ResteasyUTF8CharsetFilter.class).in(Singleton.class);

        // todo review it
        if (cfg != null) {
            String appId = cfg.xcfg().getString("messages.appId", "");
            if (StringUtils.isBlank(appId)) {
                appId = cfg.xcfg().getString("app-name", "App");
            }
            MessageException.APP_ID = appId;
        }
    }

    @Singleton
    @Provides
    public ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}

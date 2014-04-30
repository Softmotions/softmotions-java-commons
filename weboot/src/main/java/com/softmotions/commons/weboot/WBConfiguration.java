package com.softmotions.commons.weboot;


import ninja.utils.NinjaProperties;
import com.softmotions.commons.io.Loader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class WBConfiguration {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final NinjaProperties ninjaProperties;

    protected final XMLConfiguration xcfg;

    protected WBConfiguration(NinjaProperties ninjaProperties) {
        this(ninjaProperties, null, true);
    }

    protected WBConfiguration(NinjaProperties ninjaProperties, String cfgResource, boolean resource) {
        this.ninjaProperties = ninjaProperties;
        URL cfgUrl = Loader.getResourceAsUrl(cfgResource, resource ? getClass() : null);
        if (cfgUrl == null) {
            throw new RuntimeException("Unable to find configuration: " + cfgResource);
        }
        try {
            xcfg = new XMLConfiguration(cfgUrl);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public XMLConfiguration impl() {
        return xcfg;
    }

    public abstract String getEnvironmentType();

    public abstract String getDBEnvironmentType();

    public NinjaProperties getNinjaProperties() {
        return ninjaProperties;
    }
}

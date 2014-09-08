package com.softmotions.weboot;


import com.softmotions.commons.io.Loader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import javax.servlet.ServletContext;
import java.net.URL;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class WBConfiguration {

    protected XMLConfiguration xcfg;

    protected WBConfiguration() {
    }

    public void load(String location, ServletContext sctx) {
        URL cfgUrl = Loader.getResourceAsUrl(location, getClass());
        if (cfgUrl == null) {
            throw new RuntimeException("Failed to find configuration: " + location);
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
}

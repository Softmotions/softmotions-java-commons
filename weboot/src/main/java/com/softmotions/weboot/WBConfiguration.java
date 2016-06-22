package com.softmotions.weboot;


import javax.servlet.ServletContext;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Dispose;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class WBConfiguration extends ServicesConfiguration {

    protected ServletContext servletContext;

    protected WBConfiguration() {
    }

    public void load(String location, ServletContext sctx) {
        this.servletContext = sctx;
        load(location);
    }

    public void load(String location, HierarchicalConfiguration<ImmutableNode> xcfg, ServletContext sctx) {
        this.servletContext = sctx;
        load(location, xcfg);
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    protected String substituteConfigKey(String key) {
        String s = super.substituteConfigKey(key);
        if (s == null) {
            if ("webapp".equals(key)) {
                s = getServletContext().getRealPath("/");
                if (s != null) {
                    if (s.endsWith("/")) {
                        s = s.substring(0, s.length() - 1);
                    }
                }
            }
        }
        return s;
    }

    @Override
    @Dispose(order = 1)
    public void dispose() {
        super.dispose();
    }

    public abstract String getEnvironmentType();

    public abstract String getDBEnvironmentType();
}

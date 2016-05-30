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
    public String substitutePath(String path) {
        path = super.substitutePath(path);
        if (path == null) {
            return null;
        }
        String webappPath = getServletContext().getRealPath("/");
        if (webappPath != null) {
            if (webappPath.endsWith("/")) {
                webappPath = webappPath.substring(0, webappPath.length() - 1);
            }
            path = path.replace("{webapp}", webappPath);
        }
        return path;
    }

    @Override
    @Dispose(order = 1)
    public void dispose() {
        super.dispose();
    }

    public abstract String getEnvironmentType();

    public abstract String getDBEnvironmentType();
}

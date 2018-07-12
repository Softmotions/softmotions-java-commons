package com.softmotions.weboot;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;

import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Dispose;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class WBConfiguration extends ServicesConfiguration {

    private final Properties coreProps;

    private String appPrefix;

    private String appRoot;

    private String environmentType;

    private String dbEnvironment;

    protected ServletContext servletContext;

    protected WBConfiguration() {
        String cpr = getCorePropsLocationResource();
        coreProps = new Properties();
        if (cpr != null) {
            try (InputStream is = getClass().getResourceAsStream(cpr)) {
                if (is == null) {
                    throw new RuntimeException("Jar resource not found: " + cpr);
                }
                coreProps.load(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void load(String location, ServletContext sctx) {
        this.servletContext = sctx;
        load(location);
        normalizePrefix("site-files-root");
        normalizePrefix("app-prefix");
        this.appPrefix = xcfg.getString("app-prefix", "");
        this.appRoot = sctx.getContextPath() + this.appPrefix;
        this.environmentType = xcfg.getString("environment", "production");
        this.dbEnvironment = xcfg.getString("db-environment", "production");
    }


    protected String getCorePropsLocationResource() {
        return null;
    }

    public void load(String location, HierarchicalConfiguration<ImmutableNode> xcfg, ServletContext sctx) {
        this.servletContext = sctx;
        load(location, xcfg);
    }

    public String getAppVersion() {
        return coreProps.getProperty("project.version");
    }

    public Properties getCoreProperties() {
        return coreProps;
    }

    @Nonnull
    public String getApplicationName() {
        return xcfg().getString("app-name", "App");
    }

    @Nullable
    public String getLogoutRedirect() {
        String ret = xcfg().getString("logout-redirect", null);
        if (StringUtils.isBlank(ret)) {
            return xcfg.getString("site.root", null);
        }
        return ret;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    @Nonnull
    public String getAppPrefix() {
        return appPrefix;
    }

    @Nonnull
    public String getAppRoot() {
        return appRoot;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public String getDBEnvironmentType() {
        return dbEnvironment;
    }

    public boolean isTesting() {
        return "test".equals(getEnvironmentType());
    }

    public boolean isProduction() {
        return (getEnvironmentType() == null || "production".equals(getEnvironmentType()));
    }

    public boolean isDevelopment() {
        return "dev".equals(getEnvironmentType());
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

    @Nonnull
    public String getAbsoluteLink(HttpServletRequest req, String link) {
        boolean preferRequestUrl = xcfg().getBoolean("site.preferRequestUrl", true);
        if (preferRequestUrl) {
            //noinspection MagicNumber
            link = req.getScheme() + "://" +
                   req.getServerName() +
                   (req.getServerPort() != 80 && req.getServerPort() != 443 ? ":" + req.getServerPort() : "") +
                   link;
        } else {
            link = xcfg().getString("site.root") + link;
        }
        return link;
    }

    private void normalizePrefix(String property) {
        String val = xcfg().getString(property, null);
        if (StringUtils.isBlank(val) || "/".equals(val)) {
            val = "";
        } else {
            val = val.trim();
            if (!val.startsWith("/")) {
                val = '/' + val;
            }
            if (val.endsWith("/")) {
                val = val.substring(0, val.length() - 1);
            }
        }
        xcfg().setProperty(property, val);
    }

    @Override
    @Dispose(order = 1)
    public void dispose() {
        super.dispose();
    }
}

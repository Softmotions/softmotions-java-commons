package com.softmotions.weboot;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Dispose;
import com.softmotions.xconfig.XConfig;

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
        this.appPrefix = xcfg.textPattern("app-prefix", "");
        this.appRoot = sctx.getContextPath() + this.appPrefix;
        this.environmentType = xcfg.textPattern("environment", "production");
        this.dbEnvironment = xcfg.textPattern("db-environment", "production");
    }


    protected String getCorePropsLocationResource() {
        return null;
    }

    public void load(String location, XConfig xcfg, ServletContext sctx) {
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
        return xcfg().textPattern("app-name", "App");
    }

    @Nullable
    public String getLogoutRedirect() {
        String ret = xcfg().text("logout-redirect");
        if (StringUtils.isBlank(ret)) {
            return xcfg.text("site.root");
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
        boolean preferRequestUrl = xcfg().boolPattern("site.preferRequestUrl", true);
        if (preferRequestUrl) {
            String scheme = (req.getHeader("X-Forwarded-Proto") != null) ?
                            req.getHeader("X-Forwarded-Proto") : req.getScheme();
            String serverName = (req.getHeader("X-Forwarded-Host") != null) ?
                            req.getHeader("X-Forwarded-Host") : req.getServerName();
            int serverPort = (req.getHeader("X-Forwarded-Port") != null) ?
                             Integer.parseInt(req.getHeader("X-Forwarded-Port")) : req.getServerPort();
            //noinspection MagicNumber
            link = scheme + "://" +
                   serverName +
                   (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "") +
                   link;
        } else {
            link = xcfg().textPattern("site.root", "") + link;
        }
        return link;
    }

    private void normalizePrefix(String property) {
        String val = xcfg().text(property);
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
        xcfg().set(property, val);
    }

    @Override
    @Dispose(order = 1)
    public void dispose() {
        super.dispose();
    }
}

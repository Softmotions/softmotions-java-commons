package com.softmotions.weboot;

import com.softmotions.weboot.liquibase.WBLiquibaseModule;
import com.softmotions.weboot.mb.MBMyBatisModule;
import com.softmotions.weboot.solr.SolrModule;

import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Weboot engine servlet module.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class WBServletModule<C extends WBConfiguration> extends ServletModule {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private C cfg;

    public C getConfiguration() {
        return cfg;
    }

    protected void configureServlets() {
        log.info("Configuring WB modules and servlets");
        ServletContext sc = getServletContext();
        if (sc == null) {
            return;
        }
        cfg = (C) sc.getAttribute(WBServletListener.WEBOOT_CFG_SCTX_KEY);
        if (cfg == null) {
            throw new RuntimeException("Application configuration is not registered in the servlet context, " +
                                       "key: " + WBServletListener.WEBOOT_CFG_SCTX_KEY);
        }
        XMLConfiguration xcfg = cfg.impl();
        bind(WBConfiguration.class).toInstance(cfg);

        if (!xcfg.configurationsAt("mybatis").isEmpty()) {
            install(new MBMyBatisModule(cfg));
        }

        if (!xcfg.configurationsAt("liquibase").isEmpty()) {
            install(new WBLiquibaseModule());
        }

        ClassLoader cl = ObjectUtils.firstNonNull(
                Thread.currentThread().getContextClassLoader(),
                getClass().getClassLoader()
        );
        List<HierarchicalConfiguration> mconfigs = xcfg.configurationsAt("modules.module");
        for (final HierarchicalConfiguration mcfg : mconfigs) {
            String mclassName = mcfg.getString("[@class]");
            if (StringUtils.isBlank(mclassName)) {
                continue;
            }
            try {
                Class mclass = cl.loadClass(mclassName);
                if (!Module.class.isAssignableFrom(mclass)) {
                    log.warn("Module class: " + mclassName + " is not Guice module, skipped");
                    continue;
                }
                log.info("Installing '" + mclassName + "' Guice module");
                Object minst = null;

                for (Constructor c : mclass.getConstructors()) {
                    Class[] ptypes = c.getParameterTypes();
                    if (ptypes.length != 1) {
                        continue;
                    }
                    if (ptypes[0].isAssignableFrom(cfg.getClass())) {
                        try {
                            minst = c.newInstance(cfg);
                        } catch (InvocationTargetException e) {
                            log.error("", e);
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (minst == null) {
                    minst = mclass.newInstance();
                }
                install((Module) minst);
                if (minst instanceof WBServletInitializerModule) {
                    ((WBServletInitializerModule) minst).initServlets(this);
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to activate Guice module: " + mclassName, e);
            }
        }

        if (!xcfg.configurationsAt("solr").isEmpty()) {
            install(new SolrModule(cfg));
        }

        init(cfg);
    }

    protected abstract void init(C cfg);

    public ServletContext getWBServletContext() {
        return getServletContext();
    }

    public void serve(String pattern, Class<? extends HttpServlet> servletClass) {
        log.info("Serving {} with {}", pattern, servletClass);
        serve(pattern).with(servletClass);
    }

    public void serve(String pattern, Class<? extends HttpServlet> servletClass, Map<String, String> params) {
        log.info("Serving {} with {}", pattern, servletClass);
        serve(pattern).with(servletClass, params);
    }

    public void serveAndBind(String pattern, Class<? extends HttpServlet> servletClass, Map<String, String> params) {
        log.info("Serving {} with {}", pattern, servletClass);
        bind(servletClass).in(Singleton.class);
        serve(pattern).with(servletClass, params);
    }

    public void filter(String pattern, Class<? extends Filter> filterClass) {
        log.info("Filter {} with {}", pattern, filterClass);
        filter(pattern).through(filterClass);
    }

    public void filter(String pattern, Class<? extends Filter> filterClass, Map<String, String> params) {
        log.info("Filter {} with {}", pattern, filterClass);
        filter(pattern).through(filterClass, params);
    }

    public void filterAndBind(String pattern, Class<? extends Filter> filterClass, Map<String, String> params) {
        log.info("Filter {} with {}", pattern, filterClass);
        bind(filterClass).in(Singleton.class);
        filter(pattern).through(filterClass, params);
    }
}

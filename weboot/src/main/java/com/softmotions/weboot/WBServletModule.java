package com.softmotions.weboot;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.softmotions.commons.ServicesConfiguration;

/**
 * Weboot engine servlet module.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("unchecked")
public abstract class WBServletModule<C extends WBConfiguration> extends ServletModule {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private C cfg;

    public C getConfiguration() {
        return cfg;
    }

    @Override
    protected void configureServlets() {
        log.info("Configuring WB modules and servlets");
        ServletContext sc = getServletContext();
        if (sc == null) {
            return;
        }
        //noinspection unchecked
        cfg = (C) sc.getAttribute(WBServletListener.WEBOOT_CFG_SCTX_KEY);
        if (cfg == null) {
            throw new RuntimeException("Application configuration is not registered in the servlet context, " +
                                       "key: " + WBServletListener.WEBOOT_CFG_SCTX_KEY);
        }
        bind(WBConfiguration.class).toInstance(cfg);
        bind(ServicesConfiguration.class).toInstance(cfg);

        ClassLoader cl = ObjectUtils.firstNonNull(
                Thread.currentThread().getContextClassLoader(),
                getClass().getClassLoader()
        );
        List<HierarchicalConfiguration<ImmutableNode>> mconfigs = cfg.xcfg().configurationsAt("modules.module");
        for (final HierarchicalConfiguration<ImmutableNode> mcfg : mconfigs) {
            String mclassName = mcfg.getString(".");
            if (StringUtils.isBlank(mclassName)) {
                continue;
            }
            try {
                Class mclass = cl.loadClass(mclassName);
                if (!Module.class.isAssignableFrom(mclass)) {
                    log.warn("Module class: {} is not Guice module, skipped", mclassName);
                    continue;
                }
                log.info("Installing '{}' Guice module", mclassName);
                Object minst = null;

                for (Constructor c : mclass.getConstructors()) {
                    Class[] ptypes = c.getParameterTypes();
                    if (ptypes.length != 1) {
                        continue;
                    }
                    //noinspection unchecked
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
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to activate Guice module: " + mclassName, e);
            }
        }

        init(cfg);
    }

    @Override
    protected void install(Module module) {
        super.install(module);
        if (module instanceof WBServletInitializerModule) {
            ((WBServletInitializerModule) module).initServlets(this);
        }
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

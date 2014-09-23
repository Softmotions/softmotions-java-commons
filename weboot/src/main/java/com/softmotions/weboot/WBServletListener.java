package com.softmotions.weboot;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import com.softmotions.weboot.lifecycle.LifeCycleModule;
import com.softmotions.weboot.lifecycle.LifeCycleService;

import com.google.common.base.Objects;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Weboot engine startup listener.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class WBServletListener extends GuiceServletContextListener implements LifeCycleService {

    private static final Logger log = LoggerFactory.getLogger(WBServletListener.class);

    public static final String WEBOOT_CFG_CLASS_INITPARAM = "WEBOOT_CFG_CLASS";

    public static final String WEBOOT_CFG_LOCATION_INITPARAM = "WEBOOT_CFG_LOCATION";

    public static final String WEBOOT_CFG_SCTX_KEY = "com.softmotions.weboot.CFG";

    private Injector injector;

    public void contextInitialized(ServletContextEvent evt) {
        ServletContext sctx = evt.getServletContext();
        String cfgClassName = sctx.getInitParameter(WEBOOT_CFG_CLASS_INITPARAM);
        if (cfgClassName == null) {
            cfgClassName = System.getProperty(WEBOOT_CFG_CLASS_INITPARAM);
        }
        if (cfgClassName == null) {
            cfgClassName = System.getenv(WEBOOT_CFG_CLASS_INITPARAM);
        }
        if (cfgClassName == null) {
            throw new RuntimeException("Failed to find WEBOOT configuration class implementation " +
                                       "in [servlet context, system property, system env] " +
                                       "under the KEY: " +
                                       WEBOOT_CFG_CLASS_INITPARAM);
        }
        log.info("Using WEBOOT configuration class: " + cfgClassName);
        ClassLoader cl = Objects.firstNonNull(Thread.currentThread().getContextClassLoader(),
                                              getClass().getClassLoader());
        Class cfgClass;
        WBConfiguration cfg;
        try {
            cfgClass = cl.loadClass(cfgClassName);
            if (!WBConfiguration.class.isAssignableFrom(cfgClass)) {
                throw new RuntimeException("Configuration implementation must extend the: " + WBConfiguration.class + " class");
            }
            cfg = (WBConfiguration) cfgClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Failed to load/instantiate WEBOOT configuration class: " + cfgClassName, e);
        }
        sctx.setAttribute(WEBOOT_CFG_SCTX_KEY, cfg);

        String cfgLocation = sctx.getInitParameter(WEBOOT_CFG_LOCATION_INITPARAM);
        if (cfgLocation == null) {
            cfgLocation = System.getProperty(WEBOOT_CFG_LOCATION_INITPARAM);
        }
        if (cfgLocation == null) {
            cfgLocation = System.getenv(WEBOOT_CFG_LOCATION_INITPARAM);
        }
        if (cfgLocation == null) {
            throw new RuntimeException("Failed to find WEBOOT configuration location " +
                                       "in [servlet context, system property, system env] " +
                                       "under the KEY: " +
                                       WEBOOT_CFG_LOCATION_INITPARAM);
        }


        cfg.load(cfgLocation, sctx);

        //init logging
        String lref = cfg.xcfg().getString("logging[@ref]");
        if (!StringUtils.isBlank(lref)) {
            String pdir = FilenameUtils.getPath(cfgLocation);
            String lcfg = pdir + lref;
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            URL url = ref2Url(lcfg);
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset();
                configurator.doConfigure(url);
            } catch (JoranException ignored) {
                // StatusPrinter will handle this
            }
            log.info("Successfully configured application logging from: {}", url);
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        }

        super.contextInitialized(evt);
    }

    private static URL ref2Url(String ref) {
        URL url = null;
        try {
            url = Resources.getResource(ref);
        } catch (IllegalArgumentException ignored) {
        }
        if (url == null) {
            try {
                File file = new File(ref);
                if (file.exists()) {
                    url = new File(ref).toURI().toURL();
                }
            } catch (MalformedURLException ignored) {
            }
        }
        if (url == null) {
            try {
                url = new URL(ref);
            } catch (MalformedURLException ignored) {
            }
        }
        return url;
    }

    public void contextDestroyed(ServletContextEvent evt) {
        super.contextDestroyed(evt);
        this.injector = null;
    }

    public final Injector getInjector() {
        if (injector != null) {
            return injector;
        }
        List<Module> modules = new ArrayList<>();
        modules.add(new LifeCycleModule());
        modules.addAll(getStartupModules());
        injector = Guice.createInjector(Stage.PRODUCTION, modules);
        return injector;
    }

    public void start() {
        getInjector().getInstance(LifeCycleService.class).start();
    }

    public void stop() {
        getInjector().getInstance(LifeCycleService.class).stop();
    }

    public boolean isStarted() {
        return getInjector().getInstance(LifeCycleService.class).isStarted();
    }

    protected abstract Collection<Module> getStartupModules();
}

package com.softmotions.weboot;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import com.softmotions.commons.JVMResources;
import com.softmotions.commons.cont.Pair;
import com.softmotions.commons.lifecycle.LifeCycleModule;
import com.softmotions.commons.lifecycle.LifeCycleService;

/**
 * Weboot engine startup listener.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class WBServletListener extends GuiceServletContextListener implements LifeCycleService {

    private static final Logger log = LoggerFactory.getLogger(WBServletListener.class);

    public static final String WEBOOT_CFG_CLASS_INITPARAM = "WEBOOT_CFG_CLASS";

    public static final String WEBOOT_CFG_LOCATION_INITPARAM = "WEBOOT_CFG_LOCATION";

    public static final String WEBOOT_APP_ID = "WEBOOT_APP_ID";

    public static final String WEBOOT_CFG_SCTX_KEY = "com.softmotions.weboot.CFG";

    protected Injector injector;


    private Pair<String, String> getEnvInitParam(ServletContext sctx, String pname) {
        String key, ret;
        String appId = sctx.getInitParameter(WEBOOT_APP_ID);
        appId = StringUtils.isBlank(appId) ? null : appId.toUpperCase();
        StringBuilder keys = new StringBuilder();
        do {
            key = (appId != null) ? appId + '_' + pname : pname;
            if (keys.length() > 0) {
                keys.append(", ");
            }
            keys.append(key);
            ret = sctx.getInitParameter(key);
            if (ret == null) {
                ret = System.getProperty(key);
            }
            if (ret == null) {
                ret = System.getenv(key);
            }
            if (ret == null && appId != null) {
                appId = null;
                continue;
            }
            break;
        } while (true);

        return new Pair<>(ret, keys.toString());
    }


    @Override
    public void contextInitialized(ServletContextEvent evt) {
        ServletContext sctx = evt.getServletContext();
        Pair<String, String> ret = getEnvInitParam(sctx, WEBOOT_CFG_CLASS_INITPARAM);
        if (ret.getOne() == null) {
            throw new RuntimeException("Failed to find WEBOOT configuration class implementation " +
                                       "in [servlet context, system property, system env] " +
                                       "under the keys: " + ret.getTwo());
        }
        String cfgClassName = ret.getOne();
        log.info("Using WEBOOT configuration class: {}", cfgClassName);
        ClassLoader cl = ObjectUtils.firstNonNull(Thread.currentThread().getContextClassLoader(),
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

        ret = getEnvInitParam(sctx, WEBOOT_CFG_LOCATION_INITPARAM);
        if (ret.getOne() == null) {
            throw new RuntimeException("Failed to find WEBOOT configuration location " +
                                       "in [servlet context, system property, system env] " +
                                       "under the KEYS: " + ret.getTwo());
        }
        String cfgLocation = ret.getOne();
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

    @Override
    public void contextDestroyed(ServletContextEvent evt) {
        super.contextDestroyed(evt);
        this.injector = null;
    }

    @Override
    public Injector getInjector() {
        if (injector != null) {
            return injector;
        }
        List<Module> modules = new ArrayList<>();
        modules.add(new LifeCycleModule());
        modules.addAll(getStartupModules());
        injector = Guice.createInjector(Stage.PRODUCTION, modules);
        JVMResources.set("com.softmotions.weboot.WBServletListener.Injector", injector);
        return injector;
    }

    @Override
    public void start() {
        getInjector().getInstance(LifeCycleService.class).start();
    }

    @Override
    public void stop() {
        getInjector().getInstance(LifeCycleService.class).stop();
    }

    @Override
    public boolean isStarted() {
        return getInjector().getInstance(LifeCycleService.class).isStarted();
    }

    protected abstract Collection<Module> getStartupModules();
}

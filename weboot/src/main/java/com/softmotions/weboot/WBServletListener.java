package com.softmotions.weboot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRegistration;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import com.samaxes.filter.CacheFilter;
import com.samaxes.filter.NoCacheFilter;
import com.samaxes.filter.util.CacheConfigParameter;
import com.softmotions.commons.JVMResources;
import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.cont.ArrayUtils;
import com.softmotions.commons.cont.Pair;
import com.softmotions.commons.lifecycle.LifeCycleModule;
import com.softmotions.commons.lifecycle.LifeCycleService;
import com.softmotions.web.JarResourcesFilter;

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

    protected abstract String getLogo();

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

        super.contextInitialized(evt);


        for (Map.Entry<String, ? extends FilterRegistration> e : sctx.getFilterRegistrations().entrySet()) {
            FilterRegistration sreg = e.getValue();
            for (String m : sreg.getUrlPatternMappings()) {
                log.info("{} => {} ({})", m, sreg.getName(), sreg.getClassName());
            }
        }
        for (Map.Entry<String, ? extends ServletRegistration> e : sctx.getServletRegistrations().entrySet()) {
            ServletRegistration sreg = e.getValue();
            for (String m : sreg.getMappings()) {
                log.info("{} => {} ({})", m, sreg.getName(), sreg.getClassName());
            }
        }
        log.info(getLogo(), cfg.getEnvironmentType(), cfg.getAppVersion(), Runtime.getRuntime().maxMemory());
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

    protected void initCacheHeadersFilters(WBConfiguration env, ServletContext sctx) {
        HierarchicalConfiguration<ImmutableNode> xcfg = env.xcfg();
        List<HierarchicalConfiguration<ImmutableNode>> cgroups = xcfg.configurationsAt("cache-headers-groups.cache-group");
        for (HierarchicalConfiguration cfg : cgroups) {
            String name = cfg.getString("name", "");
            String[] patterns = cfg.getStringArray("patterns");
            String prefix = env.getAppPrefix();
            for (int i = 0; i < patterns.length; ++i) {
                String p = patterns[i].trim();
                if (p.charAt(0) == '/') {
                    patterns[i] = prefix + p;
                }
            }
            if (patterns.length > 0) {
                initCacheHeadersFilter(sctx, name, patterns, cfg);
            }
        }
    }

    protected void initCacheHeadersFilter(ServletContext sctx, String name, String[] patterns, HierarchicalConfiguration cfg) {
        if (!cfg.getBoolean("nocache", false)) {
            name = "WBCacheFilter" + name;
            String fname = name;
            FilterRegistration.Dynamic reg = sctx.addFilter(fname, CacheFilter.class);
            for (int i = 1; reg == null && i < 100; ++i) {
                fname = name + i;
                reg = sctx.addFilter(fname, CacheFilter.class);
            }
            if (reg == null) {
                return;
            }
            reg.setInitParameter(CacheConfigParameter.EXPIRATION.getName(),
                                 String.valueOf(cfg.getLong(CacheConfigParameter.EXPIRATION.getName(), 60L * 60L)));
            String val = cfg.getString(CacheConfigParameter.VARY.getName());
            if (!StringUtils.isBlank(val)) {
                reg.setInitParameter(CacheConfigParameter.VARY.getName(), val.trim());
            }
            val = cfg.getString(CacheConfigParameter.MUST_REVALIDATE.getName());
            if (!StringUtils.isBlank(val)) {
                reg.setInitParameter(CacheConfigParameter.MUST_REVALIDATE.getName(), val.trim());
            }
            val = cfg.getString(CacheConfigParameter.PRIVATE.getName());
            if (!StringUtils.isBlank(val)) {
                reg.setInitParameter(CacheConfigParameter.PRIVATE.getName(), val.trim());
            }

            log.info("Cache filter: {} for patterns: {}", fname, Arrays.asList(patterns));
            reg.addMappingForUrlPatterns(null, false, patterns);

        } else {
            name = "WBNoCacheFilter" + name;
            String fname = name;
            FilterRegistration.Dynamic reg = sctx.addFilter(fname, NoCacheFilter.class);
            for (int i = 1; reg == null && i < 100; ++i) {
                fname = name + i;
                reg = sctx.addFilter(fname, NoCacheFilter.class);
            }
            if (reg == null) {
                return;
            }
            log.info("NoCache filter: {} for patterns: {}", fname, Arrays.asList(patterns));
            reg.addMappingForUrlPatterns(null, false, patterns);
        }
    }


    protected void initJarResources(WBConfiguration env, ServletContext sctx) {
        FilterRegistration.Dynamic fr = sctx.addFilter("jarResourcesFilter", JarResourcesFilter.class);
        fr.addMappingForUrlPatterns(null, false, env.getAppPrefix() + "/*");
        List<HierarchicalConfiguration<ImmutableNode>> rlist = env.xcfg().configurationsAt("jar-web-resources.resource");
        for (HierarchicalConfiguration rcfg : rlist) {
            String pp = rcfg.getString("path-prefix");
            String[] opts = rcfg.getStringArray("options");
            if (pp == null || opts.length == 0) {
                continue;
            }
            fr.setInitParameter(pp, ArrayUtils.stringJoin(opts, ","));
        }
        fr.setInitParameter("strip-prefix", env.getAppPrefix());
    }

    protected abstract Collection<Module> getStartupModules();
}

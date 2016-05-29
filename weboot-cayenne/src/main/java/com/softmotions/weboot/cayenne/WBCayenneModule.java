package com.softmotions.weboot.cayenne;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import static com.google.inject.matcher.Matchers.not;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.matcher.AbstractMatcher;
import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Dispose;
import com.softmotions.commons.lifecycle.Start;

/**
 * Apache cayenne module
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("UnnecessaryFullyQualifiedName")
public class WBCayenneModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBCayenneModule.class);

    private static final AbstractMatcher<Method> DECLARED_BY_OBJECT = new AbstractMatcher<Method>() {
        @Override
        public boolean matches(Method method) {
            //noinspection ObjectEquality
            return method.getDeclaringClass() == Object.class;
        }
    };

    private final ServicesConfiguration cfg;

    private final List<org.apache.cayenne.di.Module> extraCayenneModules;


    public WBCayenneModule(ServicesConfiguration cfg, List<org.apache.cayenne.di.Module> extraCayenneModules) {
        this.cfg = cfg;
        this.extraCayenneModules = new ArrayList<>(extraCayenneModules);
    }

    public WBCayenneModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
        this.extraCayenneModules = Collections.emptyList();
    }

    @Override
    protected void configure() {
        if (cfg.xcfg().configurationsAt("cayenne").isEmpty()) {
            log.warn("No WBCayenneModule module configuration found. Skipping.");
            return;
        }
        String cfgLocation = cfg.xcfg().getString("cayenne.config");
        if (cfgLocation == null) {
            throw new RuntimeException("Missing required 'config' attribute in the <cayenne> element");
        }
        bind(CayenneWrapper.class).toInstance(new CayenneWrapper(cfg, cfgLocation, extraCayenneModules));
        bind(CayeneInitializer.class).asEagerSingleton();
        bind(ServerRuntime.class).toProvider(CayenneRuntimeProvider.class);

        //@Transactional interceptor
        TransactionalInterceptor interceptor = new TransactionalInterceptor();
        requestInjection(interceptor);
        bindInterceptor(any(), not(DECLARED_BY_OBJECT).and(annotatedWith(Transactional.class)), interceptor);
        bindInterceptor(annotatedWith(Transactional.class), not(DECLARED_BY_OBJECT).and(not(annotatedWith(Transactional.class))), interceptor);
    }


    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    public static class CayenneWrapper {

        private final String cfgLocation;

        private final ServicesConfiguration cfg;

        private final List<org.apache.cayenne.di.Module> extraCayenneModules;

        private volatile ServerRuntime runtime;


        public CayenneWrapper(ServicesConfiguration cfg,
                              String cfgLocation,
                              List<org.apache.cayenne.di.Module> extraCayenneModules) {
            this.cfgLocation = cfgLocation;
            this.extraCayenneModules = extraCayenneModules;
            this.cfg = cfg;
        }

        @Nonnull
        ServerRuntime getRuntime(DataSource dataSource) throws Exception {
            if (runtime == null) {
                synchronized (CayenneWrapper.class) {
                    if (runtime == null) {
                        start(dataSource);
                    }
                }
            }
            return runtime;
        }

        void start(DataSource dataSource) throws Exception {
            log.info("WBCayenneModule starting cayenne runtime. Config: {}", cfgLocation);

            ClassLoader cl = ObjectUtils.firstNonNull(
                    Thread.currentThread().getContextClassLoader(),
                    getClass().getClassLoader());
            List<HierarchicalConfiguration<ImmutableNode>> mconfigs = cfg.xcfg().configurationsAt("cayenne.modules.module");
            List<org.apache.cayenne.di.Module> modules = new ArrayList<>(mconfigs.size());
            for (final HierarchicalConfiguration<ImmutableNode> mcfg : mconfigs) {
                String mclassName = mcfg.getString("class");
                if (StringUtils.isBlank(mclassName)) {
                    continue;
                }
                try {
                    Class mclass = cl.loadClass(mclassName);
                    if (!org.apache.cayenne.di.Module.class.isAssignableFrom(mclass)) {
                        log.warn("Module class: {} is not Cayenne module, skipped", mclassName);
                        continue;
                    }
                    log.info("Installing '{}' Cayenne module", mclassName);
                    Object module = null;
                    for (Constructor c : mclass.getConstructors()) {
                        Class[] ptypes = c.getParameterTypes();
                        if (ptypes.length != 1) {
                            continue;
                        }
                        //noinspection unchecked
                        if (ptypes[0].isAssignableFrom(cfg.getClass())) {
                            try {
                                module = c.newInstance(cfg);
                            } catch (InvocationTargetException e) {
                                log.error("", e);
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    if (module == null) {
                        module = mclass.newInstance();
                    }
                    modules.add((org.apache.cayenne.di.Module) module);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to activate Cayenne module: " + mclassName, e);
                }
            }

            runtime = new ServerRuntimeBuilder()
                    .addConfigs(cfgLocation)
                    .addModules(modules)
                    .addModules(extraCayenneModules)
                    .dataSource(dataSource)
                    .build();

            log.info("WBCayenneModule cayenne runtime configured");
        }

        void shutdown() throws Exception {
            if (runtime != null) {
                synchronized (CayenneWrapper.class) {
                    runtime.shutdown();
                }
            }
        }
    }

    public static class CayenneRuntimeProvider implements Provider<ServerRuntime> {

        private final CayenneWrapper cayenneWrapper;

        private final Provider<DataSource> dataSourceProvider;

        @Inject
        public CayenneRuntimeProvider(CayenneWrapper cayenneWrapper,
                                      Provider<DataSource> dataSourceProvider) {
            this.cayenneWrapper = cayenneWrapper;
            this.dataSourceProvider = dataSourceProvider;
        }

        @Override
        public ServerRuntime get() {
            ServerRuntime runtime;
            try {
                runtime = cayenneWrapper.getRuntime(dataSourceProvider.get());
            } catch (Exception e) {
                log.error("", e);
                throw new ProvisionException("CayenneRuntime is not initialized", e);
            }
            return runtime;
        }
    }


    public static class CayeneInitializer {

        private final Provider<ServerRuntime> runtimeProvider;

        private final CayenneWrapper cayenneWrapper;

        @Inject
        public CayeneInitializer(Provider<ServerRuntime> runtimeProvider,
                                 CayenneWrapper cayenneWrapper) {
            this.runtimeProvider = runtimeProvider;
            this.cayenneWrapper = cayenneWrapper;
        }

        @Start(order = 15)
        public void start() throws Exception {
            runtimeProvider.get();
        }

        @Dispose(order = 15)
        public void shutdown() throws Exception {
            cayenneWrapper.shutdown();
        }
    }
}

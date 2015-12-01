package com.softmotions.weboot.cayenne;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.commons.configuration.XMLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.softmotions.weboot.WBConfiguration;
import com.softmotions.weboot.lifecycle.Dispose;
import com.softmotions.weboot.lifecycle.Start;

/**
 * Apache cayenne module
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBCayenneModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBCayenneModule.class);

    private final WBConfiguration cfg;


    public WBCayenneModule(WBConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        if (cfg.xcfg().configurationsAt("cayenne").isEmpty()) {
            log.warn("No WBCayenneModule module configuration found. Skipping.");
            return;
        }
        XMLConfiguration xcfg = cfg.xcfg();
        String cfgLocation = xcfg.getString("cayenne[@config]");
        if (cfgLocation == null) {
            throw new RuntimeException("Missing required 'config' attribute in the <cayenne> element");
        }
        bind(CayenneWrapper.class).toInstance(new CayenneWrapper(cfgLocation));
        bind(CayeneInitializer.class).asEagerSingleton();
        bind(ServerRuntime.class).toProvider(CayenneRuntimeProvider.class);
    }


    public static class CayenneWrapper {

        final String cfgLocation;

        volatile ServerRuntime runtime;

        public CayenneWrapper(String cfgLocation) {
            this.cfgLocation = cfgLocation;
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
            runtime = new ServerRuntimeBuilder()
                    .addConfigs(cfgLocation)
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

        final CayenneWrapper cayenneWrapper;

        final Provider<DataSource> dataSourceProvider;

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

        final Provider<ServerRuntime> runtimeProvider;

        final CayenneWrapper cayenneWrapper;

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

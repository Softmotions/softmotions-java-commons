package com.softmotions.weboot.ds;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.softmotions.weboot.WBConfiguration;
import com.softmotions.weboot.WBJVMResources;
import com.softmotions.weboot.lifecycle.Dispose;
import com.softmotions.weboot.lifecycle.Start;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBDatasourceModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBDatasourceModule.class);

    private final WBConfiguration cfg;

    public WBDatasourceModule(WBConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        if (cfg.xcfg().configurationsAt("datasource").isEmpty()) {
            log.warn("No WBDatasourceModule module configuration found. Skipping.");
            return;
        }
        XMLConfiguration xcfg = cfg.xcfg();
        String propsStr = xcfg.getString("datasource");

        Properties dsProps = new Properties();
        if (!StringUtils.isBlank(propsStr)) {
            try {
                dsProps.load(new StringReader(propsStr));
            } catch (IOException e) {
                String msg = "Failed to load <datasource> properties";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        String propsFile = cfg.substitutePath(xcfg.getString("datasource[@propertiesFile]"));
        if (!StringUtils.isBlank(propsFile)) {
            log.info("WBDatasourceModule loading the properties file: {}", propsFile);
            try (FileInputStream is = new FileInputStream(propsFile)) {
                dsProps.load(is);
            } catch (IOException e) {
                log.error("Failed to load the properties file: {}", propsFile);
                throw new RuntimeException(e);
            }
        }

        Properties logProps = new Properties();
        logProps.putAll(dsProps);
        for (String k : logProps.stringPropertyNames()) {
            if (k.toLowerCase().contains("passw")) {
                logProps.setProperty(k, "********");
            }
        }
        log.info("WBDatasourceModule properties: {}", logProps);
        bind(DatasourceWrapper.class).toInstance(new DatasourceWrapper(xcfg, dsProps));
        bind(DataSource.class).toProvider(DataSourceProvider.class);
        bind(DatasourceInitializer.class).asEagerSingleton();
    }

    public static class DatasourceWrapper {

        final XMLConfiguration cfg;

        final Properties dsProps;

        volatile HikariDataSource dataSource;

        DatasourceWrapper(XMLConfiguration cfg, Properties dsProps) {
            this.dsProps = dsProps;
            this.cfg = cfg;
        }

        @Nonnull
        DataSource getDataSource() throws Exception {
            if (dataSource == null) {
                synchronized (DatasourceWrapper.class) {
                    if (dataSource == null) {
                        start();
                    }
                }
            }
            return dataSource;
        }

        void start() throws Exception {
            dataSource = new HikariDataSource(new HikariConfig(dsProps));
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(0)) {
                    log.info("Database connection to: {} successfullly opened",
                             dsProps.getProperty("jdbcUrl"));
                } else {
                    log.error("Failed to estabilish database connection to: {}",
                              dsProps.getProperty("jdbcUrl"));
                }
            }

            String jvmDsName = cfg.getString("datasource[@jvmDsName]");
            String jndiName = cfg.getString("datasource[@jndiName]");
            if (jndiName != null) {
                InitialContext initCtx = new InitialContext();
                Context comp = (Context) initCtx.lookup("java:comp");
                Context jdbc, env;
                try {
                    env = (Context) comp.lookup("env");
                } catch (NamingException e) {
                    env = comp.createSubcontext("env");
                }
                try {
                    jdbc = (Context) env.lookup("jdbc");
                } catch (NamingException e) {
                    jdbc = env.createSubcontext("jdbc");
                }
                jdbc.rebind(jndiName, dataSource);
                log.info("Datasource JNDI name: java:comp/env/jdbc/{}", jndiName);
            }
            if (jvmDsName != null) {
                WBJVMResources.set(jvmDsName, dataSource);
                log.info("Datasource registered in WBJVMDatasources as '{}'", jvmDsName);
            }
        }

        void shutdown() throws Exception {
            if (dataSource != null) {
                synchronized (DatasourceWrapper.class) {
                    if (dataSource != null) {
                        dataSource.close();
                    }
                }
            }
        }
    }

    public static class DataSourceProvider implements Provider<DataSource> {

        final DatasourceWrapper datasourceWrapper;

        @Inject
        DataSourceProvider(DatasourceWrapper datasourceWrapper) {
            this.datasourceWrapper = datasourceWrapper;
        }

        @Override
        public DataSource get() {
            DataSource ds;
            try {
                ds = datasourceWrapper.getDataSource();
            } catch (Exception e) {
                throw new ProvisionException("Datasource is not initialized", e);
            }
            return ds;
        }
    }


    public static class DatasourceInitializer {

        final DatasourceWrapper datasourceWrapper;

        @Inject
        public DatasourceInitializer(DatasourceWrapper datasourceWrapper) {
            this.datasourceWrapper = datasourceWrapper;
        }

        @Start(order = 5)
        public void start() {
            try {
                datasourceWrapper.getDataSource();
            } catch (Exception e) {
                log.error("", e);
            }
        }

        @Dispose(order = 5)
        public void shutdown() {
            try {
                datasourceWrapper.shutdown();
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }
}

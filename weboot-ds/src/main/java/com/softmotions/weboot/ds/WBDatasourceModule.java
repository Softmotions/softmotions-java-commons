package com.softmotions.weboot.ds;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.util.Properties;
import javax.annotation.Nonnull;
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
            log.info("WBDatasourceModule loading the properties file: " + propsFile);
            try (FileInputStream is = new FileInputStream(propsFile)) {
                dsProps.load(is);
            } catch (IOException e) {
                log.error("Failed to load the properties file: " + propsFile);
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
        log.info("WBDatasourceModule properties: " + logProps);
        bind(DatasourceWrapper.class).toInstance(new DatasourceWrapper(dsProps));
        bind(DataSource.class).toProvider(DataSourceProvider.class);
        bind(DatasourceInitializer.class).asEagerSingleton();
    }

    public static class DatasourceWrapper {

        final Properties dsProps;

        volatile HikariDataSource dataSource;

        DatasourceWrapper(Properties dsProps) {
            this.dsProps = dsProps;
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
                    log.info("Database connection to: "
                             + dsProps.getProperty("jdbcUrl")
                             + " successfullly opened");
                } else {
                    log.error("Failed to estabilish database connection to: "
                              + dsProps.getProperty("jdbcUrl"));
                }
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

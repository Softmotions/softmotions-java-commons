package com.softmotions.commons.weboot.mb;

import ninja.lifecycle.Dispose;
import com.softmotions.commons.weboot.WBConfiguration;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.guice.XMLMyBatisModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBMyBatisModule extends XMLMyBatisModule {

    private static final Logger log = LoggerFactory.getLogger(WBMyBatisModule.class);

    final WBConfiguration cfg;

    public WBMyBatisModule(WBConfiguration cfg) {
        this.cfg = cfg;
    }

    protected void initialize() {
        XMLConfiguration xcfg = cfg.impl();
        setEnvironmentId(cfg.getDBEnvironmentType());
        String cfgLocation = xcfg.getString("mybatis[@config]");
        if (cfgLocation == null) {
            throw new RuntimeException("Missing required 'config' attribute in <mybatis> element");
        }
        setClassPathResource(cfgLocation);

        Properties props = new Properties();
        String propsStr = xcfg.getString("mybatis");
        if (!StringUtils.isBlank(propsStr)) {
            try {
                props.load(new StringReader(propsStr));
            } catch (IOException e) {
                String msg = "Failed to load <mybatis> properties";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }
        addProperties(props);

        for (String k : props.stringPropertyNames()) {
            if (k.toLowerCase().contains("passw")) {
                props.setProperty(k, "********");
            }
        }
        log.info("MyBatis environment type: " + cfg.getDBEnvironmentType());
        log.info("MyBatis properties: " + props);
        log.info("MyBatis config: " + cfgLocation);

        if (xcfg.getBoolean("mybatis[@bindDatasource]", false)) {
            bind(DataSource.class).toProvider(DataSourceProvider.class);
        }
        bind(MyBatisInitializer.class).asEagerSingleton();
    }

    static class DataSourceProvider implements Provider<DataSource> {
        final Provider<SqlSessionFactory> sessionFactoryProvider;

        @Inject
        DataSourceProvider(Provider<SqlSessionFactory> sessionFactoryProvider) {
            this.sessionFactoryProvider = sessionFactoryProvider;
        }

        public DataSource get() {
            SqlSessionFactory sf = sessionFactoryProvider.get();
            Environment env = sf.getConfiguration().getEnvironment();
            return env.getDataSource();
        }
    }

    public static class MyBatisInitializer {

        final Provider<DataSource> dsProvider;

        @Inject
        public MyBatisInitializer(Provider<DataSource> dsProvider) {
            this.dsProvider = dsProvider;
        }

        @Dispose(order = 5)
        public void shutdown() {
            log.info("Shutting down MyBatis datasource");
            DataSource ds = dsProvider.get();
            if (ds instanceof PooledDataSource) {
                PooledDataSource pds = (PooledDataSource) ds;
                pds.forceCloseAll();
            } else {
                log.warn("Unknown datasource found: " + ds.getClass().getName() + " it will not be closed");
            }
        }
    }

}

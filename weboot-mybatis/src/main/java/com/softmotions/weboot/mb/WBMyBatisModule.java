package com.softmotions.weboot.mb;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Dispose;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBMyBatisModule extends MBXMLMyBatisModule {

    private static final Logger log = LoggerFactory.getLogger(WBMyBatisModule.class);

    private final ServicesConfiguration cfg;

    public WBMyBatisModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        if (cfg.xcfg().configurationsAt("mybatis").isEmpty()) {
            return;
        }
        super.configure();
    }

    @Override
    protected void initialize() {
        HierarchicalConfiguration<ImmutableNode> xcfg = cfg.xcfg();
        String dbenv = xcfg.getString("mybatis.dbenv", "development");
        setEnvironmentId(dbenv);
        String cfgLocation = xcfg.getString("mybatis.config");
        if (cfgLocation == null) {
            throw new RuntimeException("Missing required 'config' attribute in the <mybatis> element");
        }
        setClassPathResource(cfgLocation);

        Properties props = new Properties();
        String propsStr = xcfg.getString("mybatis.extra-properties");
        if (!StringUtils.isBlank(propsStr)) {
            try {
                props.load(new StringReader(propsStr));
            } catch (IOException e) {
                String msg = "Failed to load <mybatis> properties";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        String propsFile = xcfg.getString("mybatis.propsFile");
        if (!StringUtils.isBlank(propsFile)) {
            log.info("MyBatis loading the properties file: {}", propsFile);
            try (FileInputStream is = new FileInputStream(propsFile)) {
                props.load(is);
            } catch (IOException e) {
                log.error("Failed to load the properties file: {}", propsFile);
                throw new RuntimeException(e);
            }
        }
        addProperties(props);

        for (String k : props.stringPropertyNames()) {
            if (k.toLowerCase().contains("passw")) {
                props.setProperty(k, "********");
            }
        }

        for (HierarchicalConfiguration<ImmutableNode> mc : xcfg.configurationsAt("mybatis.extra-mappers.mapper")) {
            String resource = mc.getString("resource");
            if (!StringUtils.isBlank(resource)) {
                log.info("MyBatis registering extra mapper: '{}'", resource);
                getExtraMappers().add(resource);
            }
        }

        log.info("MyBatis environment type: {}", dbenv);
        log.info("MyBatis properties: {}", props);
        log.info("MyBatis config: {}", cfgLocation);

        if (xcfg.getBoolean("mybatis.bindDatasource", false)) {
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

        @Override
        public DataSource get() {
            SqlSessionFactory sf = sessionFactoryProvider.get();
            Environment env = sf.getConfiguration().getEnvironment();
            return env.getDataSource();
        }
    }

    public static class MyBatisInitializer {

        final Provider<DataSource> dsProvider;

        final ServicesConfiguration cfg;

        @Inject
        public MyBatisInitializer(Provider<DataSource> dsProvider,
                                  ServicesConfiguration cfg) {
            this.dsProvider = dsProvider;
            this.cfg = cfg;
        }

        @Dispose(order = 5)
        public void shutdown() {
            log.info("Shutting down MyBatis datasource");
            DataSource ds = dsProvider.get();
            String shutdownSql = cfg.xcfg().getString("mybatis.shutdownSQL");
            if (ds != null && !StringUtils.isBlank(shutdownSql)) {
                log.info("Executing shutdown SQL: '{}" + '\'', shutdownSql);
                try (Connection c = ds.getConnection()) {
                    try (Statement stmt = c.createStatement()) {
                        stmt.execute(shutdownSql);
                    }
                } catch (Exception e) {
                    log.error("", e);
                }
            }
            if (ds instanceof PooledDataSource) {
                PooledDataSource pds = (PooledDataSource) ds;
                pds.forceCloseAll();
            } else {
                log.warn("Unknown datasource found: {} it will not be closed", ds.getClass().getName());
            }
        }
    }

}

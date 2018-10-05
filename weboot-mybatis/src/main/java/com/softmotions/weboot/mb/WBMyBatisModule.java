package com.softmotions.weboot.mb;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.ibatis.io.Resources.getResourceAsReader;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
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
        Multibinder.newSetBinder(binder(), WBMyBatisExtraConfigSupplier.class);
        var xcfg = cfg.xcfg();
        if (!xcfg.hasPattern("mybatis")) {
            return;
        }
        String dbenv = xcfg.textPattern("mybatis.dbenv", "development");
        setEnvironmentId(dbenv);
        String cfgLocation = xcfg.text("mybatis.config");
        if (cfgLocation == null) {
            throw new RuntimeException("Missing required 'config' attribute in the <mybatis> element");
        }
        log.info("MyBatis config: {}", cfgLocation);
        setClassPathResource(cfgLocation);
        super.configure();
        if (xcfg.boolPattern("mybatis.bindDatasource", false)) {
            bind(DataSource.class).toProvider(DataSourceProvider.class);
        }
        bind(WBMyBatisModule.class).toInstance(this);
        bind(SqlSessionFactory.class).toProvider(SqlSessionFactoryProvider.class).in(Singleton.class);
        bind(MyBatisInitializer.class).asEagerSingleton();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected void configureEagerSessionFactory() {
        // we do not want eager session factory building
        // implemented own SqlSessionFactoryProvider for late initialization
    }

    protected SqlSessionFactory initialize(Set<WBMyBatisExtraConfigSupplier> extraConfigSuppliers) throws Exception {
        var xcfg = cfg.xcfg();
        Properties props = new Properties();
        String propsStr = xcfg.text("mybatis.extra-properties");
        if (!StringUtils.isBlank(propsStr)) {
            try {
                props.load(new StringReader(propsStr));
            } catch (IOException e) {
                String msg = "Failed to load <mybatis> properties";
                log.error(msg, e);
                throw new RuntimeException(msg, e);
            }
        }

        String propsFile = xcfg.text("mybatis.propsFile");
        if (!StringUtils.isBlank(propsFile)) {
            log.info("MyBatis loading the properties file: {}", propsFile);
            try (FileInputStream is = new FileInputStream(propsFile)) {
                props.load(is);
            } catch (IOException e) {
                log.error("Failed to load the properties file: {}", propsFile);
                throw new RuntimeException(e);
            }
        }

        //
        // todo fixme! Here is a durty assumptions:
        String jdbcDriver = props.getProperty("JDBC.driver");
        if (jdbcDriver != null && jdbcDriver.contains("DB2Driver")) {
            props.setProperty("SQL.TRUE.LITERAL", "1");
            props.setProperty("SQL.FALSE.LITERAL", "0");
        } else {
            props.setProperty("SQL.TRUE.LITERAL", "true");
            props.setProperty("SQL.FALSE.LITERAL", "false");
        }

        addProperties(props);

        for (var mc : xcfg.subPattern("mybatis.extra-mappers.mapper")) {
            String resource = mc.text("resource");
            if (!StringUtils.isBlank(resource)) {
                log.info("MyBatis registering extra mapper: '{}'", resource);
                getExtraMappers().add(resource);
            }
        }

        for (WBMyBatisExtraConfigSupplier ecs : extraConfigSuppliers) {
            for (String resource : ecs.extraMappersXML()) {
                log.info("MyBatis registering extra mapper: '{}' from: '{}'", resource, ecs);
                getExtraMappers().add(resource);
            }
        }

        log.info("MyBatis environment type: {}", getEnvironmentId());
        StringBuilder pb = new StringBuilder();
        props.stringPropertyNames().stream().sorted().forEach(k -> {
            String pv = props.getProperty(k);
            if (k.toLowerCase().contains("passw")) {
                pv = "********";
            }
            pb.append(System.getProperty("line.separator")).append("    ")
                    .append(k).append('=').append(pv);
        });
        log.info("MyBatis properties: {}", pb);

        SqlSessionFactory sessionFactory;
        try (Reader reader = getResourceAsReader(getResourceClassLoader(), getClassPathResource())) {
            sessionFactory =
                    new ExtendedSqlSessionFactoryBuilder()
                            .build(reader,
                                   getEnvironmentId(),
                                   getProperties());
        }

        return sessionFactory;
    }


    public static class SqlSessionFactoryProvider implements Provider<SqlSessionFactory> {

        private final WBMyBatisModule module;

        private final Set<WBMyBatisExtraConfigSupplier> extraConfigSuppliers;

        @Inject
        public SqlSessionFactoryProvider(WBMyBatisModule module,
                                         Set<WBMyBatisExtraConfigSupplier> extraConfigSuppliers) {
            this.module = module;
            this.extraConfigSuppliers = extraConfigSuppliers;
        }

        @Override
        public SqlSessionFactory get() {
            try {
                return module.initialize(extraConfigSuppliers);
            } catch (Exception e) {
                log.error("", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static class DataSourceProvider implements Provider<DataSource> {

        private final Provider<SqlSessionFactory> sessionFactoryProvider;

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

        private final Provider<DataSource> dsProvider;

        private final ServicesConfiguration cfg;

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
            String shutdownSql = cfg.xcfg().text("mybatis.shutdownSQL");
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

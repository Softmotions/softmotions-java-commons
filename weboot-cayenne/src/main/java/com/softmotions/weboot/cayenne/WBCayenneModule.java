package com.softmotions.weboot.cayenne;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.configuration.server.ServerRuntimeBuilder;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
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
                    .addModule(new CayenneJava8Module())
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


    public static class CayenneJava8Module implements Module {

        @Override
        public void configure(Binder binder) {
            binder
                    .bindList(Constants.SERVER_DEFAULT_TYPES_LIST)
                    .add(new LocalDateType())
                    .add(new LocalTimeType())
                    .add(new LocalDateTimeType());
        }
    }

    public static class LocalDateTimeType implements ExtendedType {

        @Override
        public String getClassName() {
            return LocalDateTime.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement statement, Object value, int pos, int type, int scale) throws Exception {
            statement.setTimestamp(pos, Timestamp.valueOf((LocalDateTime) value));
        }

        @Override
        public LocalDateTime materializeObject(ResultSet rs, int index, int type) throws Exception {
            Timestamp timestamp = rs.getTimestamp(index);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }

        @Override
        public Object materializeObject(CallableStatement rs, int index, int type) throws Exception {
            Timestamp timestamp = rs.getTimestamp(index);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }
    }


    public static class LocalDateType implements ExtendedType {

        @Override
        public String getClassName() {
            return LocalDate.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement statement, Object value, int pos, int type, int scale) throws Exception {
            statement.setDate(pos, Date.valueOf((LocalDate) value));
        }

        @Override
        public LocalDate materializeObject(ResultSet rs, int index, int type) throws Exception {
            Date date = rs.getDate(index);
            return date != null ? date.toLocalDate() : null;
        }

        @Override
        public LocalDate materializeObject(CallableStatement rs, int index, int type) throws Exception {
            Date date = rs.getDate(index);
            return date != null ? date.toLocalDate() : null;
        }
    }

    public static class LocalTimeType implements ExtendedType {

        @Override
        public String getClassName() {
            return LocalTime.class.getName();
        }

        @Override
        public void setJdbcObject(PreparedStatement statement, Object value, int pos, int type, int scale) throws Exception {
            statement.setTime(pos, Time.valueOf((LocalTime) value));
        }

        @Override
        public LocalTime materializeObject(ResultSet rs, int index, int type) throws Exception {
            Time time = rs.getTime(index);
            return time != null ? time.toLocalTime() : null;
        }

        @Override
        public Object materializeObject(CallableStatement rs, int index, int type) throws Exception {
            Time time = rs.getTime(index);
            return time != null ? time.toLocalTime() : null;
        }

    }


}

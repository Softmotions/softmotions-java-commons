package com.softmotions.weboot.mongo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.annotation.Nonnull;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.mongodb.Bytes;
import com.mongodb.DB;
import com.mongodb.DBAddress;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.gridfs.GridFS;
import com.softmotions.commons.ServicesConfiguration;

/**
 * Weboot mongodb module.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBMongoModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBMongoModule.class);

    private final ServicesConfiguration cfg;

    public WBMongoModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        if (cfg.xcfg().configurationsAt("mongo").isEmpty()) {
            return;
        }
        bind(WBMongo.class).toProvider(WBMongoProvider.class).in(Singleton.class);
        bind(Jongo.class).toProvider(JongoProvider.class).in(Singleton.class);
        bind(Mongo.class).toProvider(MongoProvider.class).in(Singleton.class);
        bind(DB.class).toProvider(DBProvider.class).in(Singleton.class);
        bind(GridFS.class).toProvider(GFSProvider.class).in(Singleton.class);
    }

    public static class GFSProvider implements Provider<GridFS> {

        private final Provider<WBMongo> mp;

        @Inject
        GFSProvider(Provider<WBMongo> mp) {
            this.mp = mp;
        }

        @Override
        public GridFS get() {
            return new GridFS(mp.get().getDefaultDB());
        }
    }


    public static class DBProvider implements Provider<DB> {

        private final Provider<WBMongo> mp;

        @Inject
        DBProvider(Provider<WBMongo> mp) {
            this.mp = mp;
        }

        @Override
        public DB get() {
            return mp.get().getDefaultDB();
        }
    }


    public static class MongoProvider implements Provider<Mongo> {

        private final Provider<WBMongo> mp;

        @Inject
        MongoProvider(Provider<WBMongo> mp) {
            this.mp = mp;
        }

        @Override
        public Mongo get() {
            return mp.get().getMongo();
        }
    }


    public static class JongoProvider implements Provider<Jongo> {

        private final Provider<WBMongo> mp;

        @Inject
        JongoProvider(Provider<WBMongo> mp) {
            this.mp = mp;
        }

        @Override
        public Jongo get() {
            return mp.get().getJongo();
        }
    }


    public static class WBMongoProvider implements Provider<WBMongo> {

        private final Properties props;

        private volatile WBMongo wbMongo;

        @Inject
        public WBMongoProvider(ServicesConfiguration cfg) {
            this(cfg, cfg.xcfg());
        }

        public WBMongoProvider(ServicesConfiguration cfg, HierarchicalConfiguration<ImmutableNode> xcfg) {
            this.props = new Properties();
            String propsStr = xcfg.getString("mongo.properties");
            if (!StringUtils.isBlank(propsStr)) {
                try {
                    props.load(new StringReader(propsStr));
                } catch (IOException e) {
                    String msg = "Failed to load <mongo/properties> properties";
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
            String propsFile = xcfg.getString("mongo.propsFile");
            if (!StringUtils.isBlank(propsFile)) {
                log.info("WBMongoModule loading the properties file: {}", propsFile);
                try (FileInputStream is = new FileInputStream(propsFile)) {
                    props.load(is);
                } catch (IOException e) {
                    log.error("Failed to load the properties file: {}", propsFile);
                    throw new RuntimeException(e);
                }
            }

            Properties logProps = new Properties();
            logProps.putAll(props);
            for (String k : logProps.stringPropertyNames()) {
                if (k.toLowerCase().contains("passw")) {
                    logProps.setProperty(k, "********");
                }
            }
            log.info("WBMongoModule properties: {}", logProps);
            if (props.getProperty("connectionUrl") == null) {
                throw new RuntimeException("WBMongoModule: Missing required configuration property: 'connectionUrl'");
            }
        }

        @Override
        public WBMongo get() {
            if (wbMongo != null) {
                return wbMongo;
            }
            synchronized (WBMongoProvider.class) {
                if (wbMongo != null) {
                    return wbMongo;
                }
                try {
                    wbMongo = init();
                } catch (Exception e) {
                    log.error("", e);
                    throw new RuntimeException(e);
                }
            }
            return wbMongo;
        }

        WBMongo init() throws Exception {
            return new WBMongoImpl(props);
        }
    }


    public static class WBMongoImpl implements WBMongo {

        private final String defDbName;

        private final MongoClient mongo;

        private final Mapper mapper;

        WBMongoImpl(Properties props) throws Exception {
            String connectionUrl = props.getProperty("connectionUrl");
            if (connectionUrl == null) {
                throw new RuntimeException("WBMongoModule: Missing required configuration property: 'connectionUrl'");
            }
            List<MongoCredential> credentials = new ArrayList<>();
            if (props.getProperty("user") != null) {
                String authDB = props.getProperty("authDB");
                if (StringUtils.isBlank(authDB)) {
                    authDB = "admin";
                }
                String pw = props.getProperty("password");
                if (pw == null) {
                    pw = "";
                }
                pw = pw.trim();
                credentials.add(
                        MongoCredential.createMongoCRCredential(props.getProperty("user"),
                                                                authDB,
                                                                pw.toCharArray()));
            }
            DBAddress dba = new DBAddress(connectionUrl);
            MongoClient client = new MongoClient(dba, credentials);
            if (BooleanUtils.toBoolean(props.getProperty("slaveOK"))) {
                client.addOption(Bytes.QUERYOPTION_SLAVEOK);
            }
            this.defDbName = dba.getDBName();
            this.mongo = client;
            this.mapper = new JacksonMapper.Builder().build();
        }

        @Override
        @Nonnull
        public Mongo getMongo() {
            return mongo;
        }

        @Override
        @Nonnull
        public Jongo getJongo(String dbName) {
            return new Jongo(mongo.getDB(dbName), mapper);
        }

        @Override
        @Nonnull
        public Jongo getJongo() {
            return new Jongo(getDefaultDB(), mapper);
        }

        @Override
        @Nonnull
        public DB getDefaultDB() {
            return mongo.getDB(defDbName);
        }
    }
}

package com.softmotions.weboot.mongo;

import com.softmotions.weboot.WBConfiguration;

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

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jongo.marshall.jackson.JacksonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Weboot mongodb module.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBMongoModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBMongoModule.class);

    private final WBConfiguration cfg;

    public WBMongoModule(WBConfiguration cfg) {
        this.cfg = cfg;
    }

    protected void configure() {
        XMLConfiguration xcfg = cfg.xcfg();
        if (xcfg.configurationsAt("mongo").isEmpty()) {
            return;
        }
        bind(WBMongo.class).toProvider(WBMongoProvider.class).in(Singleton.class);
    }


    static class WBMongoProvider implements Provider<WBMongo> {

        private final Properties props;

        private volatile WBMongo wbMongo;

        @Inject
        WBMongoProvider(WBConfiguration cfg) {
            this.props = new Properties();
            XMLConfiguration xcfg = cfg.xcfg();
            String propsStr = xcfg.getString("mongo");
            if (!StringUtils.isBlank(propsStr)) {
                try {
                    props.load(new StringReader(propsStr));
                } catch (IOException e) {
                    String msg = "Failed to load <mongo> properties";
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
            String propsFile = cfg.substitutePath(xcfg.getString("mongo[@propsFile]"));
            if (!StringUtils.isBlank(propsFile)) {
                log.info("WBMongoModule loading the properties file: " + propsFile);
                try (FileInputStream is = new FileInputStream(propsFile)) {
                    props.load(is);
                } catch (IOException e) {
                    log.error("Failed to load the properties file: " + propsFile);
                    throw new RuntimeException(e);
                }
            }
            SubnodeConfiguration mcfg = xcfg.configurationAt("mongo");
            for (ConfigurationNode a : mcfg.getRootNode().getAttributes()) {
                if (!props.containsKey(a.getName())) {
                    props.setProperty(a.getName(), (String) a.getValue());
                }
            }
            Properties logProps = new Properties(props);
            for (String k : logProps.stringPropertyNames()) {
                if (k.toLowerCase().contains("passw")) {
                    logProps.setProperty(k, "********");
                }
            }
            log.info("WBMongoModule properties: " + props);
            if (props.getProperty("connectionUrl") == null) {
                throw new RuntimeException("WBMongoModule: Missing required configuration property: 'connectionUrl'");
            }
        }

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


    static class WBMongoImpl implements WBMongo {

        private final String defDbName;

        private final Mongo mongo;

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
                MongoCredential.createMongoCRCredential(props.getProperty("user"),
                                                        authDB,
                                                        pw.toCharArray());
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

        @Nonnull
        public Mongo getMongo() {
            return mongo;
        }

        @Nonnull
        public Jongo getJongo(String dbName) {
            return new Jongo(mongo.getDB(dbName), mapper);
        }

        @Nonnull
        public Jongo getJongo() {
            return new Jongo(getDefaultDB(), mapper);
        }

        @Nonnull
        public DB getDefaultDB() {
            return mongo.getDB(defDbName);
        }
    }
}

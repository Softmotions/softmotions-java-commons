package com.softmotions.weboot.mongo;

import com.softmotions.weboot.WBConfiguration;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.mongodb.DB;
import com.mongodb.Mongo;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.commons.lang3.StringUtils;
import org.jongo.Jongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
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

    /*
     defaultDB;
     database=localhost/
        user=nsusite
        password=
        authDB=portal
     */

    protected void configure() {
        XMLConfiguration xcfg = cfg.xcfg();
        if (xcfg.configurationsAt("mongo").isEmpty()) {
            return;
        }
        Properties props = new Properties();
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

        bind(WBMongo.class).toProvider(new WBMongoProvider(props)).in(Singleton.class);
    }


    static class WBMongoProvider implements Provider<WBMongo> {

        private final Properties props;

        private volatile WBMongo wbMongo;

        WBMongoProvider(Properties props) {
            this.props = props;
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

        WBMongoImpl(Properties props) throws Exception {


        }

        @Nonnull
        public Mongo getMongo() {
            return null;
        }

        @Nonnull
        public Jongo getJongo(String dbName) {
            return null;
        }

        @Nonnull
        public Jongo getJongo() {
            return null;
        }

        @Nonnull
        public DB getDefaultDB() {
            return null;
        }
    }


}

package com.softmotions.web.security;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.util.Hashtable;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class XMLWSUserDatabaseJNDIFactory extends AbstractWSUserDatabaseJNDIFactory {

    private static final Logger log = LoggerFactory.getLogger(XMLWSUserDatabaseJNDIFactory.class);

    public XMLWSUserDatabaseJNDIFactory() {
        super(WSUserDatabase.class.getName(),
              XMLWSUserDatabaseJNDIFactory.class.getName(),
              null);
    }

    public Object getObjectInstance(Object obj, Name name,
                                    Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        if ((obj == null) || !(obj instanceof Reference)) {
            return null;
        }
        WSUserDatabase db;
        Reference ref = (Reference) obj;
        if (!WSUserDatabase.class.getName().equals(ref.getClassName())) {
            return null;
        }
        synchronized (DB_CACHE) {
            db = DB_CACHE.get(name);
            if (db != null) {
                return db;
            }
            boolean autoSave = false;
            String config = null;
            RefAddr ra = ref.get("config");
            if (ra != null) {
                config = ra.getContent().toString();
            }
            ra = ref.get("autoSave");
            if (ra != null) {
                autoSave = BooleanUtils.toBoolean(ra.getContent().toString());
            }
            if (config == null) {
                throw new RuntimeException("Missing required 'config' parameter");
            }
            log.info("Using database configuration: " + config);
            log.info("autoSave: " + autoSave);
            db = new WSUserDatabaseWrapper(new XMLWSUserDatabase(name.toString(), config, autoSave));
            DB_CACHE.put(name, (WSUserDatabaseWrapper) db);
        }
        return db;
    }

    public XMLWSUserDatabaseJNDIFactory setConfig(String config) {
        StringRefAddr addr = (StringRefAddr) get("config");
        if (addr != null) {
            throw new RuntimeException("'config' already set on XMLWSUserDatabaseJNDIFactory, can't be changed");
        }
        add(new StringRefAddr("config", config));
        return this;
    }

    public XMLWSUserDatabaseJNDIFactory setAutosave(boolean autosave) {
        StringRefAddr addr = (StringRefAddr) get("autosave");
        if (addr != null) {
            throw new RuntimeException("'autosave' already set on XMLWSUserDatabaseJNDIFactory, can't be changed");
        }
        add(new StringRefAddr("autosave", String.valueOf(autosave)));
        return this;
    }
}

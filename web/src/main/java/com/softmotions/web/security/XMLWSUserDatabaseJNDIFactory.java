package com.softmotions.web.security;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class XMLWSUserDatabaseJNDIFactory extends Reference implements ObjectFactory {

    private static final Logger log = LoggerFactory.getLogger(XMLWSUserDatabaseJNDIFactory.class);

    @SuppressWarnings("StaticCollection")
    private static final Map<Name, WSUserDatabaseWrapper> DB_CACHE = new HashMap<>();

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

    private static class WSUserDatabaseWrapper implements WSUserDatabase {

        private final WSUserDatabase wrapped;

        private WSUserDatabaseWrapper(WSUserDatabase wrapped) {
            this.wrapped = wrapped;
        }

        public String getDatabaseName() {
            return wrapped.getDatabaseName();
        }

        public Iterator<WSGroup> getGroups() {
            return wrapped.getGroups();
        }

        public Iterator<WSRole> getRoles() {
            return wrapped.getRoles();
        }

        public Iterator<WSUser> getUsers() {
            return wrapped.getUsers();
        }

        public WSGroup createGroup(String s, String s2) {
            return wrapped.createGroup(s, s2);
        }

        public WSRole createRole(String s, String s2) {
            return wrapped.createRole(s, s2);
        }

        public WSUser createUser(String s, String s2, String s3) {
            return wrapped.createUser(s, s2, s3);
        }

        public WSGroup findGroup(String s) {
            return wrapped.findGroup(s);
        }

        public WSRole findRole(String s) {
            return wrapped.findRole(s);
        }

        public WSUser findUser(String s) {
            return wrapped.findUser(s);
        }

        public void removeGroup(WSGroup wsGroup) {
            wrapped.removeGroup(wsGroup);
        }

        public void removeRole(WSRole wsRole) {
            wrapped.removeRole(wsRole);
        }

        public void removeUser(WSUser wsUser) {
            wrapped.removeUser(wsUser);
        }

        public int hashCode() {
            return wrapped.hashCode();
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            return wrapped.equals(obj);
        }

        public void close() throws IOException {
            synchronized (DB_CACHE) {
                List<Name> names = new ArrayList<>();
                for (final Map.Entry<Name, WSUserDatabaseWrapper> e : DB_CACHE.entrySet()) {
                    if (e.getValue().equals(this)) {
                        names.add(e.getKey());
                    }
                }
                for (final Name n : names) {
                    names.remove(n);
                }
            }
        }
    }
}

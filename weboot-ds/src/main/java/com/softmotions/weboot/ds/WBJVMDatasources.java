package com.softmotions.weboot.ds;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;

/**
 * JVM wide datasources store.
 * (Simple alternative to JNDI on some cases)
 *
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBJVMDatasources {

    @SuppressWarnings("StaticCollection")
    private static final Map<String, DataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();

    private WBJVMDatasources() {
    }

    static void set(String name, DataSource ds) {
        DATA_SOURCE_MAP.put(name, ds);
    }

    public static DataSource get(String name) {
        return DATA_SOURCE_MAP.get(name);
    }

    public static DataSource getOrFail(String name) {
        DataSource ds = get(name);
        if (ds == null) {
            throw new RuntimeException("Unknown datasource: " + name);
        }
        return ds;
    }
}

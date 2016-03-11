package com.softmotions.weboot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBJVMResources {

    private static final Logger log = LoggerFactory.getLogger(WBJVMResources.class);

    @SuppressWarnings("StaticCollection")
    private static final Map<String, Object> RESOURCES_MAP = new ConcurrentHashMap<>();

    private WBJVMResources() {
    }

    public static void set(String name, Object val) {
        log.info("Set resource {}", name);
        RESOURCES_MAP.put(name, val);
    }

    public static <T> T get(String name) {
        //noinspection unchecked
        return (T) RESOURCES_MAP.get(name);
    }

    public static <T> T getOrFail(String name) {
        T val = get(name);
        if (val == null) {
            throw new WBJVMResourcesNotFound("Unknown resource: " + name);
        }
        return val;
    }

    public static void remove(String name) {
        log.info("Remove resource {}", name);
        RESOURCES_MAP.remove(name);
    }
}

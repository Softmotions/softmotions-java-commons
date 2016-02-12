package com.softmotions.weboot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBJVMResources {

    @SuppressWarnings("StaticCollection")
    private static final Map<String, Object> RESOURCES_MAP = new ConcurrentHashMap<>();

    private WBJVMResources() {
    }

    public static void set(String name, Object val) {
        RESOURCES_MAP.put(name, val);
    }

    public static <T> T get(String name) {
        return (T) RESOURCES_MAP.get(name);
    }

    public static <T> T getOrFail(String name) {
        T val = get(name);
        if (val == null) {
            throw new RuntimeException("Unknown resource: " + name);
        }
        return val;
    }
}

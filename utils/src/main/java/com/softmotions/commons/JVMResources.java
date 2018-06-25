package com.softmotions.commons;

import java.lang.reflect.Method;
import java.net.URLStreamHandlerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("unchecked")
public class JVMResources {

    private static final Logger log = LoggerFactory.getLogger(JVMResources.class);

    @SuppressWarnings("StaticCollection")
    private static final Map<String, Object> RESOURCES_MAP = new ConcurrentHashMap<>();

    private static final Object lock = new Object();

    static {
        try {
            Class<?> clazz = Class.forName("org.apache.catalina.webresources.TomcatURLStreamHandlerFactory");
            Method m = clazz.getMethod("getInstance");
            Object hf = m.invoke(null);
            clazz.getMethod("addUserFactory", URLStreamHandlerFactory.class).invoke(hf, new JVMResourceUrlHandlerFactory());
            log.info("URLStreamHandlerFactory for jvmr:// protocol successfully registered");
        } catch (Exception e) {
            log.warn("TomcatURLStreamHandlerFactory not found: {}", e.toString());
        }
    }
    
    private JVMResources() {
    }

    public static void set(String name, Object val) {
        log.info("Set resource {}", name);
        synchronized (lock) {
            RESOURCES_MAP.put(name, val);
            lock.notifyAll();
        }
    }

    public static <T> T get(String name) {
        //noinspection unchecked
        return (T) RESOURCES_MAP.get(name);
    }

    @javax.annotation.Nullable
    public static <T> T getWait(String name, long units, TimeUnit tu) {
        T res = get(name);
        if (res != null) {
            return res;
        }
        long u = tu.toMillis(units);
        long s = System.currentTimeMillis();
        while (res == null && u > 0) {
            synchronized (lock) {
                try {
                    lock.wait(u);
                } catch (InterruptedException ignored) {
                    ;
                }
                long ns = System.currentTimeMillis();
                u -= (ns - s);
                s = ns;
                res = (T) RESOURCES_MAP.get(name);
            }
        }
        return res;
    }


    public static <T> T getOrFail(String name) {
        T val = get(name);
        if (val == null) {
            throw new JVMResourcesNotFound("Unknown resource: " + name);
        }
        return val;
    }

    public static void remove(String name) {
        log.info("Remove resource {}", name);
        synchronized (lock) {
            RESOURCES_MAP.remove(name);
            lock.notifyAll();
        }
    }
}

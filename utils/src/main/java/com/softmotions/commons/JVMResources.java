package com.softmotions.commons;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class JVMResources {

    private static final Logger log = LoggerFactory.getLogger(JVMResources.class);

    @SuppressWarnings("StaticCollection")
    private static final Map<String, Object> RESOURCES_MAP = new ConcurrentHashMap<>();

    //private static final Set<String> LOCKED_KEYS = new HashSet<>();

    static {
        //noinspection ConstantConditions
        URL.setURLStreamHandlerFactory(protocol -> "jvmr".equals(protocol) ? new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                //noinspection InnerClassTooDeeplyNested
                return new URLConnection(url) {

                    @Override
                    public void connect() throws IOException {
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        Object res = get(url.getPath());
                        if (res == null) {
                            throw new IOException("Null value of JVM resource: " + url);
                        }
                        //noinspection ChainOfInstanceofChecks
                        if (res instanceof URI) {
                            return ((URI) res).toURL().openStream();
                        }
                        if (res instanceof URL) {
                            return ((URL) res).openStream();
                        }
                        if (res instanceof Path) {
                            res = ((Path) res).toFile();
                        }
                        if (res instanceof File) {
                            return new FileInputStream((File) res);
                        }
                        if (res instanceof byte[]) {
                            return new ByteArrayInputStream((byte[]) res);
                        }
                        if (res instanceof Byte[]) {
                            return new ByteArrayInputStream(ArrayUtils.toPrimitive((Byte[]) res));
                        }
                        return IOUtils.toInputStream(res.toString(), "UTF-8");
                    }
                };
            }
        } : null);
    }


    private JVMResources() {
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
            throw new JVMResourcesNotFound("Unknown resource: " + name);
        }
        return val;
    }

    public static void remove(String name) {
        log.info("Remove resource {}", name);
        RESOURCES_MAP.remove(name);
    }
}

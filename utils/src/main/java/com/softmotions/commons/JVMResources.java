package com.softmotions.commons;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
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
    
    static {

        //todo duty hack :(

        URLStreamHandlerFactory of;
        Field ff;
        try {
            ff = URL.class.getDeclaredField("factory");
            ff.setAccessible(true);
            of = (URLStreamHandlerFactory) ff.get(null);
        } catch (Exception e) {
            throw new Error(e);
        }

        URLStreamHandlerFactory f = new URLStreamHandlerFactory() {
            @Override
            public @Nullable URLStreamHandler createURLStreamHandler(String protocol) {
                return "jvmr".equals(protocol) ? new URLStreamHandler() {
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
                } : (of != null ? of.createURLStreamHandler(protocol) : null);
            }
        };

        if (of != null) {
            try {
                ff.set(null, f);
            } catch (Exception e) {
                throw new Error(e);
            }
        } else {
            URL.setURLStreamHandlerFactory(f);
        }
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

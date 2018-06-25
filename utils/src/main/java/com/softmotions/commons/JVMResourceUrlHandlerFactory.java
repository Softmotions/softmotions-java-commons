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
import java.net.URLStreamHandlerFactory;
import java.nio.file.Path;
import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class JVMResourceUrlHandlerFactory implements URLStreamHandlerFactory {

    @Nullable
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return "jvmr".equals(protocol) ? new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                //noinspection InnerClassTooDeeplyNested
                return new URLConnection(url) {

                    @Override
                    public void connect() {
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        Object res = JVMResources.get(url.getPath());
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
        } : null;
    }
}

package com.softmotions.commons;

import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;
import javax.annotation.Nullable;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class JVMResourcesUrlHandlerFactoryProvider extends URLStreamHandlerProvider {

    static final JVMResourceUrlHandlerFactory factory = new JVMResourceUrlHandlerFactory();

    @Nullable
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return factory.createURLStreamHandler(protocol);
    }
}

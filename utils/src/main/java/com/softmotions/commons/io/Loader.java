package com.softmotions.commons.io;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Adaptive resource loader.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class Loader {

    private static final Logger log = LoggerFactory.getLogger(Loader.class);

    public static URL getResourceAsUrl(String location, Class owner) {
        if (location == null) {
            return null;
        }
        URL url = null;
        if (owner != null) {
            ClassLoader cl =
                    ObjectUtils.firstNonNull(Thread.currentThread().getContextClassLoader(),
                                             owner.getClassLoader());
            url = cl.getResource(location);
        }
        if (url == null) {
            InputStream is = null;
            try {
                url = new URL(location);
                is = url.openStream();
            } catch (IOException e) {
                url = null;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
            File cfgFile = new File(location);
            if (cfgFile.exists()) {
                try {
                    url = cfgFile.toURI().toURL();
                } catch (MalformedURLException e) {
                    log.error("", e);
                }
            }
        }
        return url;
    }
}

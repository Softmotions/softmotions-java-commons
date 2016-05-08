package com.softmotions.commons;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.commons.io.DirUtils;
import com.softmotions.commons.io.Loader;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ServicesConfiguration {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected XMLConfiguration xcfg;

    protected File tmpdir;

    protected volatile File sessionTmpDir;

    public ServicesConfiguration() {
    }

    public ServicesConfiguration(String location) {
        load(location);
    }


    protected void load(String location) {
        URL cfgUrl = Loader.getResourceAsUrl(location, getClass());
        if (cfgUrl == null) {
            throw new RuntimeException("Failed to find configuration: " + location);
        }
        log.info("Using configuration: {}", cfgUrl);
        try {
            xcfg = new XMLConfiguration(cfgUrl);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        }

        String dir = xcfg.getString("tmpdir");
        if (StringUtils.isBlank(dir)) {
            dir = System.getProperty("java.io.tmpdir");
        }
        tmpdir = new File(dir);
        log.info("Using TMP dir: {}", tmpdir.getAbsolutePath());
        try {
            DirUtils.ensureDir(tmpdir, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public XMLConfiguration xcfg() {
        return xcfg;
    }

    /**
     * System-wide tmp dir.
     */
    public File getTmpdir() {
        return tmpdir;
    }

    /**
     * Session tmp dir.
     */
    public File getSessionTmpdir() {
        if (sessionTmpDir != null) {
            return sessionTmpDir;
        }
        synchronized (ServicesConfiguration.class) {
            if (sessionTmpDir == null) {
                try {
                    sessionTmpDir = Files.createTempDirectory("weboot-").toFile();
                } catch (IOException e) {
                    log.error("", e);
                }
            }
        }
        return sessionTmpDir;
    }

    public String substitutePath(String path) {
        if (path == null) {
            return null;
        }
        path = path.replace("{cwd}", System.getProperty("user.dir"))
                   .replace("{home}", System.getProperty("user.home"))
                   .replace("{tmp}", getTmpdir().getAbsolutePath());

        if (path.contains("{newtmp}")) {
            path = path.replace("{newtmp}", getSessionTmpdir().getAbsolutePath());
        }
        return path;
    }

    public void dispose() {
        synchronized (ServicesConfiguration.class) {
            if (sessionTmpDir != null) {
                try {
                    FileUtils.deleteDirectory(sessionTmpDir);
                } catch (IOException e) {
                    log.error("", e);
                }
            }
        }
    }
}

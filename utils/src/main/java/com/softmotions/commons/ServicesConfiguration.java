package com.softmotions.commons;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.common.io.Resources;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.softmotions.commons.io.DirUtils;
import com.softmotions.commons.io.Loader;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ServicesConfiguration implements Module {

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
            Parameters params = new Parameters();
            xcfg = new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
                    .configure(
                            params.xml()
                                  .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                                  .setURL(cfgUrl)
                                  .setValidating(false))
                    .getConfiguration();

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

        //init logging
        String lref = xcfg().getString("logging-ref");
        if (!StringUtils.isBlank(lref)) {
            String pdir = FilenameUtils.getPath(location);
            String lcfg = pdir + lref;
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            URL url = ref2Url(lcfg);
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset();
                configurator.doConfigure(url);
            } catch (JoranException ignored) {
                // StatusPrinter will handle this
            }
            log.info("Successfully configured application logging from: {}", url);
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        }
    }

    private static URL ref2Url(String ref) {
        URL url = null;
        try {
            url = Resources.getResource(ref);
        } catch (IllegalArgumentException ignored) {
        }
        if (url == null) {
            try {
                File file = new File(ref);
                if (file.exists()) {
                    url = new File(ref).toURI().toURL();
                }
            } catch (MalformedURLException ignored) {
            }
        }
        if (url == null) {
            try {
                url = new URL(ref);
            } catch (MalformedURLException ignored) {
            }
        }
        return url;
    }

    public HierarchicalConfiguration<ImmutableNode> xcfg() {
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


    @Override
    public void configure(Binder binder) {
        binder.bind(ServicesConfiguration.class).toInstance(this);
    }
}

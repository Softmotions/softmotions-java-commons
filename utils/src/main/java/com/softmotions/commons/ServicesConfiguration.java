package com.softmotions.commons;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.softmotions.commons.io.DirUtils;
import com.softmotions.commons.io.Loader;
import com.softmotions.xconfig.XConfig;
import com.softmotions.xconfig.XConfigBuilder;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ServicesConfiguration implements Module {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected XConfig xcfg;

    protected final File tmpdir = new File(System.getProperty("java.io.tmpdir"));

    private boolean usedCustomLoggingConfig;

    protected volatile File sessionTmpDir;

    public ServicesConfiguration() {
    }

    public ServicesConfiguration(String location) {
        load(location);
    }

    public ServicesConfiguration(String location, XConfig xcfg) {
        load(location, xcfg);
    }

    public ServicesConfiguration(URL cfgUrl) {
        load(cfgUrl);
    }

    public ServicesConfiguration(URL cfgUrl, XConfig xcfg) {
        load(cfgUrl, xcfg);
    }

    protected void load(String location) {
        URL cfgUrl = Loader.getResourceAsUrl(location, getClass());
        if (cfgUrl == null) {
            throw new RuntimeException("Failed to find configuration: " + location);
        }
        load(cfgUrl);
    }

    protected void load(String location, XConfig xcfg) {
        this.xcfg = xcfg;
        URL cfgUrl = Loader.getResourceAsUrl(location, getClass());
        if (cfgUrl == null) {
            throw new RuntimeException("Failed to find configuration: " + location);
        }
        load(cfgUrl, xcfg);
    }

    protected void load(URL cfgUrl, XConfig xcfg) {
        this.xcfg = xcfg;
        init(cfgUrl);
    }

    protected void load(URL cfgUrl) {
        log.info("Using configuration: {}", cfgUrl);
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext) {
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rl = ctx.getLogger("ROOT");
            rl.setLevel(Level.INFO);
        }
        try {
            xcfg = new XConfigBuilder(cfgUrl)
                    .substitutor(key -> {
                        String v = substituteConfigKey(key);
                        return v != null ? v : XConfigBuilder.Companion.basicSubstitutor(key);
                    })
                    .create();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        init(cfgUrl);
    }
    
    @Nullable
    protected String substituteConfigKey(String key) {
        switch (key) {
            case "tmp":
                return getTmpdir().getAbsolutePath();
            case "newtmp":
                return getSessionTmpdir().getAbsolutePath();
            default:
                return null;
        }
    }

    public boolean isUsedCustomLoggingConfig() {
        return usedCustomLoggingConfig;
    }

    protected void init(URL cfgUrl) {
        try {
            //init logging
            String lref = xcfg().text("logging-ref");
            if (!StringUtils.isBlank(lref) && LoggerFactory.getILoggerFactory() instanceof LoggerContext) {
                String pdir = FilenameUtils.getFullPath(cfgUrl.getPath());
                String lcfg = pdir + lref;
                LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
                String protocol = cfgUrl.getProtocol();
                URL url = "jvmr".equals(protocol)
                          ? new URL(lcfg)
                          : new URL(protocol, cfgUrl.getHost(), cfgUrl.getPort(), lcfg);
                try {
                    JoranConfigurator configurator = new JoranConfigurator();
                    configurator.setContext(context);
                    context.reset();
                    configurator.doConfigure(url);
                } catch (Exception ignored) {
                    // StatusPrinter will handle this
                }
                usedCustomLoggingConfig = true;
                log.info("Successfully configured application logging from: {}", url);
                StatusPrinter.printInCaseOfErrorsOrWarnings(context);
            }
            log.info("Using TMP dir: {}", tmpdir.getAbsolutePath());
            DirUtils.ensureDir(tmpdir, true);

            if (xcfg().boolPattern("newtmp-cleanup-on-exit", true)) {
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File stmp = sessionTmpDir;
                        if (stmp != null && stmp.isDirectory()) {
                            log.info("Delete newtmp dir: {}", stmp.getAbsolutePath());
                            try {
                                FileUtils.deleteDirectory(stmp);
                            } catch (IOException e) {
                                log.error("", e);
                            }
                        }
                    }
                }));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public XConfig xcfg() {
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

    /**
     * !!!! todo  It is workaround for commons-configuration2 bug: attribute values are not splitted!
     */
    public String[] attrArray(String av) {
        if (av == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        if (av.indexOf(',') != -1) {
            String[] ret = StringUtils.split(av, ',');
            for (int i = 0, l = ret.length; i < l; ++i) {
                ret[i] = ret[i].trim();
            }
            return ret;
        } else {
            return new String[]{av};
        }
    }


    @Override
    public void configure(Binder binder) {
        binder.bind(ServicesConfiguration.class).toInstance(this);
    }
}

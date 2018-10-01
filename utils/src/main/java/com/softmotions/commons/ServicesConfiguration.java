package com.softmotions.commons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ServicesConfiguration implements Module {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected HierarchicalConfiguration<ImmutableNode> xcfg;

    protected FileBasedConfigurationBuilder<XMLConfiguration> xcfgBuilder;

    protected final File tmpdir = new File(System.getProperty("java.io.tmpdir"));

    private boolean usedCustomLoggingConfig;

    protected volatile File sessionTmpDir;

    public ServicesConfiguration() {
    }

    public ServicesConfiguration(String location) {
        load(location);
    }

    public ServicesConfiguration(String location,
                                 HierarchicalConfiguration<ImmutableNode> xcfg) {
        load(location, xcfg);
    }

    public ServicesConfiguration(URL cfgUrl) {
        load(cfgUrl);
    }

    public ServicesConfiguration(URL cfgUrl,
                                 HierarchicalConfiguration<ImmutableNode> xcfg) {
        load(cfgUrl, xcfg);
    }

    protected void load(String location) {
        URL cfgUrl = Loader.getResourceAsUrl(location, getClass());
        if (cfgUrl == null) {
            throw new RuntimeException("Failed to find configuration: " + location);
        }
        load(cfgUrl);
    }

    protected void load(String location, HierarchicalConfiguration<ImmutableNode> xcfg) {
        this.xcfg = xcfg;
        URL cfgUrl = Loader.getResourceAsUrl(location, getClass());
        if (cfgUrl == null) {
            throw new RuntimeException("Failed to find configuration: " + location);
        }
        load(cfgUrl, xcfg);
    }

    protected void load(URL cfgUrl, HierarchicalConfiguration<ImmutableNode> xcfg) {
        this.xcfg = xcfg;
        init(cfgUrl);
    }

    protected void load(URL cfgUrl) {
        log.info("Using configuration: {}", cfgUrl);
        try (InputStream is = cfgUrl.openStream()) {
            JVMResources.set(cfgUrl.toString(), preprocessConfigData(IOUtils.toString(is, "UTF-8")));
            cfgUrl = new URL("jvmr:" + cfgUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Level oldLevel = null;
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext) {
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rl = ctx.getLogger("ROOT");
            oldLevel = rl.getLevel();
            rl.setLevel(Level.ERROR);
        }
        try {
            xcfgBuilder = new FileBasedConfigurationBuilder<>(XMLConfiguration.class);
            configure(cfgUrl);
            xcfg = xcfgBuilder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } finally {
            if (oldLevel != null) {
                LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
                ctx.getLogger("ROOT").setLevel(oldLevel);
            }
        }
        init(cfgUrl);
    }

    protected void configure(URL cfgUrl) {
        Parameters params = new Parameters();
        xcfgBuilder.configure(
                params.xml()
                        .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                        .setURL(cfgUrl)
                        .setValidating(false));
    }

    private String preprocessConfigData(String cdata) {
        Pattern p = Pattern.compile("(.)?\\{(((env|sys):)?[A-Za-z_.]+)}");
        Matcher m = p.matcher(cdata);
        StringBuffer sb = new StringBuffer(cdata.length());
        while (m.find()) {
            String mg = m.group();
            String pc = m.group(1);
            if ("$".equals(pc)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(mg));
            } else {
                String s = StringUtils.replaceEach(substituteConfigKey(m.group(2)), new String[]{"\\"}, new String[]{"\\\\"});
                if (s != null) {
                    if (pc != null) {
                        m.appendReplacement(sb, Matcher.quoteReplacement(pc + s));
                    } else {
                        m.appendReplacement(sb, Matcher.quoteReplacement(s));
                    }
                } else {
                    m.appendReplacement(sb, Matcher.quoteReplacement(mg));
                }
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    protected String substituteConfigKey(String key) {
        switch (key) {
            case "cwd":
                return System.getProperty("user.dir");
            case "home":
                return System.getProperty("user.home");
            case "tmp":
                return getTmpdir().getAbsolutePath();
            case "newtmp":
                return getSessionTmpdir().getAbsolutePath();
            default:
                if (key.startsWith("env:")) {
                    return System.getenv(key.substring("env:".length()));
                } else if (key.startsWith("sys:")) {
                    return System.getProperty(key.substring("sys:".length()));
                }
        }
        return null;
    }

    public boolean isUsedCustomLoggingConfig() {
        return usedCustomLoggingConfig;
    }

    protected void init(URL cfgUrl) {
        try {
            //init logging
            String lref = xcfg().getString("logging-ref");
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

            if (xcfg().getBoolean("newtmp-cleanup-on-exit", true)) {
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

package com.softmotions.weboot.testing.jetty;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Attributes;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("unchecked")
public class JettyRunner {

    private Logger log = LoggerFactory.getLogger(JettyRunner.class);

    private Server jetty;

    private Builder builder;

    private JettyRunner() {
    }


    public Builder usedBuilder() {
        return builder;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void doConfigure(Builder b) throws Exception {

        this.builder = b;
        //System.setProperty("org.apache.jasper.compiler.disablejsr199", "false");

        jetty = new Server(b.port);
        jetty.setStopAtShutdown(true);
        WebAppContext context = new WebAppContext();
        context.setContextPath(b.contextPath);
        context.setClassLoader(getClass().getClassLoader());
        if (b.resourcesBase != null) {
            context.setResourceBase(b.resourcesBase);
        }
        if (b.initPararams != null) {
            for (Map.Entry<String, String> p : b.initPararams.entrySet()) {
                context.setInitParameter(p.getKey(), p.getValue());
            }
        }

        ClassLoader jspClassLoader = new URLClassLoader(new URL[0], this.getClass().getClassLoader());
        context.setClassLoader(jspClassLoader);

        Configuration.ClassList classlist = Configuration.ClassList.setServerDefault(jetty);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration");
        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",
                           "org.eclipse.jetty.plus.webapp.EnvConfiguration",
                           "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                             ".*/[^/]*servlet-api-[^/]*\\.jar$|.*/javax.servlet.jsp.jstl-.*\\.jar$|.*/[^/]*taglibs.*\\.jar$");


        Path jspTmp = Files.createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "jettytmp-");
        log.info("Using JSP tmp dir: {}", jspTmp);
        context.setAttribute("javax.servlet.context.tempdir", jspTmp.toAbsolutePath().toString());


        //todo https://github.com/jetty-project/embedded-jetty-jsp

        jetty.setHandler(context);
    }

    public <T> T getContextEventListener(Class<T> type) throws Exception {
        WebAppContext wh = (WebAppContext) jetty.getHandler();
        for (EventListener el : wh.getEventListeners()) {
            if (type.isAssignableFrom(el.getClass())) {
                return (T) el;
            }
        }
        throw new Exception("Unable to find ContextEventListener of type: " + type);
    }

    public void start() throws Exception {
        jetty.start();
        log.info("Jetty server started");
    }

    public void shutdown() throws Exception {
        if (jetty != null) {
            Object tmpDir = null;
            try {
                tmpDir = ((Attributes) jetty.getHandler()).getAttribute("javax.servlet.context.tempdir");
                jetty.getHandler().stop();
                jetty.stop();
                jetty.join();
                jetty.destroy();
            } finally {
                if (tmpDir != null) {
                    FileUtils.deleteDirectory((tmpDir instanceof File) ? (File) tmpDir : new File((String) tmpDir));
                }
                jetty = null;
            }
        }
        log.info("Jetty server stopped");
    }


    public Server getJetty() {
        return jetty;
    }

    public static class Builder {

        private int port = 8282;

        private String contextPath = "/";

        private String resourcesBase;

        private Map<String, String> initPararams;


        public Builder withInitParameter(String name, String value) {
            if (initPararams == null) {
                initPararams = new HashMap<>();
            }
            initPararams.put(name, value);
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }


        public Builder withContextPath(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public Builder withResourcesBase(String resourcesBase) {
            this.resourcesBase = resourcesBase;
            return this;
        }

        public JettyRunner build() throws Exception {
            JettyRunner runner = new JettyRunner();
            runner.doConfigure(this);
            return runner;
        }

        public int getPort() {
            return port;
        }

        public String getContextPath() {
            return contextPath;
        }

        public String getResourcesBase() {
            return resourcesBase;
        }

        public Map<String, String> getInitPararams() {
            return initPararams;
        }
    }
}

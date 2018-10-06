package com.softmotions.weboot.templates;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.commons.ThreadUtils;
import com.softmotions.commons.cont.CollectionUtils;
import com.softmotions.commons.lifecycle.Start;
import com.softmotions.weboot.i18n.I18n;
import com.softmotions.xconfig.XConfig;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

@ThreadSafe
public class TemplateServiceImpl implements TemplateService, LogChute {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private VelocityEngine engine;

    private final I18n i18n;

    private String templatesBase;

    private static final ThreadLocal<Boolean> SUPPRESS_NOTFOUND_ERROR = ThreadUtils.createThreadLocal();

    @Inject
    public TemplateServiceImpl(I18n i18n, XConfig xcfg) {
        this.i18n = i18n;
        this.templatesBase = xcfg.textPattern("templates.base", "/");
        if (!templatesBase.endsWith("/")) {
            templatesBase += '/';
        }

        List<String> directives =
                Arrays.stream(xcfg.arrPattern("templates.directives"))
                        .map(String::valueOf)
                        .collect(Collectors.toList());

        String loader = xcfg.text("templates.loader");

        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(TemplateServiceImpl.class.getClassLoader());
            Properties props = new Properties();
            props.setProperty("input.encoding", "UTF-8");
            props.setProperty("output.encoding", "UTF-8");
            props.setProperty("velocimacro.permissions.allow.inline.local.scope", "true");

            String loaders = "";
            if (!StringUtils.isBlank(loader)) {
                props.setProperty("custom.resource.loader.class", loader.trim());
                loaders = "custom,";
            }
            // Use bundle loader as last
            loaders += "bundle";
            props.setProperty("resource.loader", loaders);
            props.setProperty("bundle.resource.loader.class",
                              "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            if (!directives.isEmpty()) {
                props.setProperty("userdirective", CollectionUtils.join(",", directives));
            }
            if (log.isDebugEnabled()) {
                log.debug("Configuration: {}", props);
            }
            engine = new VelocityEngine(props);
            engine.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, this);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Start
    public void init() {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(TemplateServiceImpl.class.getClassLoader());
            engine.init();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public TemplateContext get(String templateName, Locale locale) throws TemplateServiceException {
        if (locale == null) {
            locale = i18n.getLocale(null);
        }
        try {
            Template template;
            try {
                SUPPRESS_NOTFOUND_ERROR.set(Boolean.TRUE);
                template = engine.getTemplate(getTemplateLocation(templateName, locale.getLanguage()));
            } catch (ResourceNotFoundException e) {
                SUPPRESS_NOTFOUND_ERROR.set(Boolean.FALSE);
                template = engine.getTemplate(getTemplateLocation(templateName, null));
            } finally {
                SUPPRESS_NOTFOUND_ERROR.set(Boolean.FALSE);
            }
            return new TemplateContextImpl(template);
        } catch (ResourceNotFoundException e) {
            throw new TemplateServiceException("Resource not found: " + templateName, e);
        } catch (ParseErrorException e) {
            log.error("", e);
            throw new TemplateServiceException("Invalid template: " + templateName, e);
        }
    }

    private String getTemplateLocation(String template, String lng) {
        if (lng == null) {
            return templatesBase + template + ".vm";
        }
        return templatesBase + template + '_' + lng.toLowerCase() + ".vm";
    }


    @Override
    public void init(RuntimeServices rs) throws Exception {
    }

    @Override
    public void log(int level, String message) {
        log(level, message, null);
    }

    @Override
    public void log(int level, String message, Throwable t) {
        if (message != null
            && SUPPRESS_NOTFOUND_ERROR.get() == Boolean.TRUE
            && message.indexOf("ResourceManager : unable to find resource") == 0) {
            return;
        }
        switch (level) {
            case LogChute.WARN_ID:
                log.warn(message, t);
                break;
            case LogChute.INFO_ID:
                log.info(message, t);
                break;
            case LogChute.DEBUG_ID:
                log.debug(message, t);
                break;
            case LogChute.TRACE_ID:
                log.trace(message, t);
                break;
            case LogChute.ERROR_ID:
                log.error(message, t);
                break;
            default:
                log.warn(message, t);
                break;
        }
    }

    @Override
    public boolean isLevelEnabled(int level) {
        switch (level) {
            case LogChute.WARN_ID:
                return log.isWarnEnabled();
            case LogChute.INFO_ID:
                return log.isInfoEnabled();
            case LogChute.DEBUG_ID:
                return log.isDebugEnabled();
            case LogChute.TRACE_ID:
                return log.isTraceEnabled();
            case LogChute.ERROR_ID:
                return log.isErrorEnabled();
            default:
                return false;
        }
    }

    /**
     * @author Adamansky Anton (adamansky@gmail.com)
     */
    public static class TemplateContextImpl implements TemplateContext {

        private final Template template;

        private final VelocityContext vctx;

        TemplateContextImpl(Template template) {
            this.template = template;
            this.vctx = new VelocityContext();
        }


        public Template getVelocityTemplate() {
            return template;
        }

        @Override
        public TemplateContext put(String key, Object val) {
            vctx.put(key, val);
            return this;
        }

        @Override
        public Object get(String key) {
            return vctx.get(key);
        }

        @Override
        public Object remove(String key) {
            return vctx.remove(key);
        }

        @Override
        public String[] keys() {
            return Arrays.stream(vctx.getKeys())
                    .map(String::valueOf)
                    .toArray(String[]::new);
        }

        @Override
        public String render() throws TemplateServiceException {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(TemplateContextImpl.class.getClassLoader());
            try {
                StringWriter sw = new StringWriter();
                template.merge(vctx, sw);
                return sw.toString().trim();
            } catch (ResourceNotFoundException | ParseErrorException | MethodInvocationException e) {
                throw new TemplateServiceException("Error renderign template", e);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }
}

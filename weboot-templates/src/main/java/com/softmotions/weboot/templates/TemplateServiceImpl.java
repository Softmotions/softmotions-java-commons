package com.softmotions.weboot.templates;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.weboot.i18n.I18n;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

@ThreadSafe
public class TemplateServiceImpl implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateServiceImpl.class);

    private VelocityEngine engine;

    private final I18n i18n;

    private String templatesBase;


    @Inject
    public TemplateServiceImpl(I18n i18n, XMLConfiguration xcfg) {
        this.i18n = i18n;
        this.templatesBase = xcfg.getString("templates.base", "/");
        if (!templatesBase.endsWith("/")) {
            templatesBase += '/';
        }
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(TemplateServiceImpl.class.getClassLoader());
            Properties props = new Properties();
            props.setProperty("input.encoding", "UTF-8");
            props.setProperty("output.encoding", "UTF-8");
            props.setProperty("velocimacro.permissions.allow.inline.local.scope", "true");
            props.setProperty("resource.loader", "bundle");
            props.setProperty("bundle.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            engine = new VelocityEngine(props);
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
                template = engine.getTemplate(getTemplateLocation(templateName, locale.getLanguage()));
            } catch (ResourceNotFoundException e) {
                template = engine.getTemplate(getTemplateLocation(templateName, null));
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
                return sw.toString();
            } catch (ResourceNotFoundException | ParseErrorException | MethodInvocationException e) {
                throw new TemplateServiceException("Error renderign template", e);
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        }
    }
}

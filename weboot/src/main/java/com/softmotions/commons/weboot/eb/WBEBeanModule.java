package com.softmotions.commons.weboot.eb;

import com.softmotions.commons.weboot.WBConfiguration;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Entity;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;

/**
 * Ebean module integration.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBEBeanModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBEBeanModule.class);

    protected void configure() {
        bind(EbeanServer.class).toProvider(EbeanProvider.class).in(Singleton.class);
    }

    public static class EbeanProvider implements Provider<EbeanServer> {

        @Inject
        Injector injector;

        @Inject
        WBConfiguration cfg;

        public EbeanServer get() {
            ServerConfig scfg = new ServerConfig();
            SubnodeConfiguration ebeanCfg = cfg.impl().configurationAt("ebean");
            String propsStr = cfg.impl().getString("ebean");
            if (ebeanCfg.getBoolean("useGuiceProvidedDatasource")) {
                scfg.setDataSource(injector.getInstance(DataSource.class));
            }
            if (propsStr != null) {
                Properties cprops = new Properties();
                try {
                    cprops.load(new StringReader(propsStr));
                    BeanUtils.populate(scfg, (Map) cprops);
                } catch (IllegalAccessException | InvocationTargetException | IOException e) {
                    String msg = "Failed to load <ebean> properties";
                    log.error(msg, e);
                    throw new RuntimeException(msg, e);
                }
            }
            if (ebeanCfg.getBoolean("scanGuiceEntities")) {
                for (final Binding<?> b : injector.getBindings().values()) {
                    final Type type = b.getKey().getTypeLiteral().getType();
                    if (type instanceof Class) {
                        if (AnnotationResolver.getClassWithAnnotation((Class<?>) type, Entity.class) != null) {
                            log.info("Register EBean entity: " + ((Class<?>) type).getName());
                            scfg.addClass((Class<?>) type);
                        }
                    }
                }
            }
            log.info("Creating EbeanServer instance");
            return EbeanServerFactory.create(scfg);
        }
    }

    private static class AnnotationResolver {

        public static Class getClassWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
            if (clazz.isAnnotationPresent(annotation)) {
                return clazz;
            }
            for (Class intf : clazz.getInterfaces()) {
                if (intf.isAnnotationPresent(annotation)) {
                    return intf;
                }
            }
            Class superClass = clazz.getSuperclass();
            //noinspection ObjectEquality
            if (superClass != Object.class && superClass != null) {
                //noinspection TailRecursion
                return getClassWithAnnotation(superClass, annotation);
            }
            return null;
        }
    }
}

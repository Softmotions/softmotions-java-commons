package com.softmotions.weboot.solr;

import com.softmotions.weboot.WBConfiguration;
import com.softmotions.weboot.lifecycle.Dispose;
import com.softmotions.weboot.lifecycle.Start;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public class SolrModule extends AbstractModule {

    protected final static Logger log = LoggerFactory.getLogger(SolrModule.class);

    private WBConfiguration cfg;

//    private Collection<SolrImportHandler> importHandlers;

    public SolrModule(WBConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        ClassLoader cl = ObjectUtils.firstNonNull(
                Thread.currentThread().getContextClassLoader(),
                getClass().getClassLoader()
        );

        try {
            XMLConfiguration xcfg = cfg.impl();
            HierarchicalConfiguration scfg = (HierarchicalConfiguration) xcfg.subset("solr");

            String providerClassName = scfg.getString("provider[@class]");
            Class<?> providerClass = cl.loadClass(providerClassName);
//        if (!SolrServerProvider.class.isAssignableFrom(providerClass)) {
////            TODO: error
//            throw new RuntimeException("provider class");
//        }
//
//        SolrServerProvider ssp = injector.getInstance((Class<? extends SolrServerProvider>) providerClass);;
//        SolrServer solr = ssp.get(scfg.configurationAt("provider"));

            bind(SolrServer.class).toProvider((Class<? extends Provider<? extends SolrServer>>) providerClass).asEagerSingleton();
            bind(SolrServerInitializer.class).asEagerSingleton();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    protected static class SolrServerInitializer {

        final Injector injector;

        final WBConfiguration cfg;

        final SolrServer solr;

        @Inject
        public SolrServerInitializer(Injector injector, WBConfiguration cfg, SolrServer solr) {
            this.injector = injector;
            this.cfg = cfg;
            this.solr = solr;
        }

        @Start
        public void start() {
            ClassLoader cl = ObjectUtils.firstNonNull(
                    Thread.currentThread().getContextClassLoader(),
                    getClass().getClassLoader()
            );

            SubnodeConfiguration scfg = cfg.impl().configurationAt("solr");

            try {
                Collection<SolrImportHandler> importHandlers = new ArrayList<>();

                for (HierarchicalConfiguration dhcfg : scfg.configurationsAt("data-handlers.data-handler")) {
                    try {
                        String dhClassName = dhcfg.getString("[@class]");
                        Class<?> dhClass = cl.loadClass(dhClassName);

                        // TODO: check
                        if (!SolrImportHandler.class.isAssignableFrom(dhClass)) {
                            throw new RuntimeException("data handler");
                        }

                        importHandlers.add(injector.getInstance((Class<? extends SolrImportHandler>) dhClass));
                    } catch (ClassNotFoundException e) {
                        // TODO:
                    }
                }

                if (scfg.getBoolean("[@rebuildIndex]", false)) {
                    rebuildIndex(solr, importHandlers);
                }

                for (SolrImportHandler importHandler : importHandlers) {
                    importHandler.init();
                }
            } catch (Exception e) {
                // TODO:
                throw new RuntimeException(e);
            }
        }

        @Dispose
        public void shutdown() {
            solr.shutdown();
        }

        /**
         * Rebuild index for all documents
         */
        private void rebuildIndex(SolrServer solr, Collection<SolrImportHandler> importHandlers) {
            try {
                solr.deleteByQuery("*:*");
                solr.commit();



                for (SolrImportHandler importHandler : importHandlers) {
                    if (solr instanceof HttpSolrServer) {
                        ((HttpSolrServer) solr).add(importHandler.getData());
                    } else {
                        for (Iterator<SolrInputDocument> iter = importHandler.getData(); iter.hasNext(); ) {
                            SolrInputDocument doc = iter.next();
                            solr.add(doc);
                        }
                    }
                    solr.commit();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}

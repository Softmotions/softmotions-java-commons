package com.softmotions.weboot.solr;

import com.softmotions.weboot.WBConfiguration;
import com.softmotions.weboot.lifecycle.Dispose;
import com.softmotions.weboot.lifecycle.Start;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
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
public class WBSolrModule extends AbstractModule {

    protected static final Logger log = LoggerFactory.getLogger(WBSolrModule.class);

    private WBConfiguration cfg;

    public WBSolrModule(WBConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        if (cfg.xcfg().configurationsAt("solr").isEmpty()) {
            return;
        }
        ClassLoader cl = ObjectUtils.firstNonNull(
                Thread.currentThread().getContextClassLoader(),
                getClass().getClassLoader()
        );

        XMLConfiguration xcfg = cfg.xcfg();
        HierarchicalConfiguration scfg = (HierarchicalConfiguration) xcfg.subset("solr");

        String providerClassName = scfg.getString("provider[@class]");
        if (StringUtils.isBlank(providerClassName)) {
            throw new RuntimeException("Missing required parameter '@class' for solr server provider");
        }
        Class<? extends Provider<? extends SolrServer>> providerClass;
        try {
            providerClass = (Class<? extends Provider<? extends SolrServer>>) cl.loadClass(providerClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Not found class for solr server provider");
        }

        bind(SolrServer.class).toProvider(providerClass).asEagerSingleton();
        bind(SolrServerInitializer.class).asEagerSingleton();
    }

    protected static class SolrServerInitializer {

        final Injector injector;

        final WBConfiguration cfg;

        final SolrServer solr;

        @Inject
        public SolrServerInitializer(Injector injector,
                                     WBConfiguration cfg,
                                     SolrServer solr) {
            this.injector = injector;
            this.cfg = cfg;
            this.solr = solr;
        }

        @Start(order = Integer.MAX_VALUE, parallel = true)
        public void start() throws Exception {
            log.info("Staring SOLR services");
            ClassLoader cl = ObjectUtils.firstNonNull(
                    Thread.currentThread().getContextClassLoader(),
                    getClass().getClassLoader()
            );

            SubnodeConfiguration scfg = cfg.xcfg().configurationAt("solr");
            Collection<SolrDataHandler> dataHandlers = new ArrayList<>();
            Collection<SolrDataHandler> autoImportHandlers = new ArrayList<>();

            for (HierarchicalConfiguration dhcfg : scfg.configurationsAt("data-handlers.data-handler")) {
                String dhClassName = dhcfg.getString("[@class]");
                Class<?> dhClass = cl.loadClass(dhClassName);

                if (!SolrDataHandler.class.isAssignableFrom(dhClass)) {
                    throw new RuntimeException("Invalid class for solr data handler");
                }

                SolrDataHandler dataHandler = injector.getInstance((Class<? extends SolrDataHandler>) dhClass);
                dataHandler.init(dhcfg);
                dataHandlers.add(dataHandler);

                if (dhcfg.getBoolean("[@autoimport]", false)) {
                    autoImportHandlers.add(dataHandler);
                }
            }

            boolean rebuild = scfg.getBoolean("[@rebuildIndex]", false);
            if (rebuild || checkEmptyIndex()) {
                rebuildIndex(dataHandlers);
            } else {
                initImport(autoImportHandlers);
            }
        }

        @Dispose(order = Integer.MAX_VALUE)
        public void shutdown() {
            log.info("Shutting down SOLR server");
            Binding<SolrServer> sb = injector.getExistingBinding(Key.get(SolrServer.class));
            if (sb != null) {
                SolrServer s = sb.getProvider().get();
                if (s != null) {
                    s.shutdown();
                }
            }
        }

        /**
         * Checks if solr index is empty
         */
        private boolean checkEmptyIndex() throws Exception {
            ModifiableSolrParams params = new ModifiableSolrParams();
            params.add(CommonParams.Q, "*:*");
            params.add(CommonParams.ROWS, "1");
            QueryResponse queryResponse = solr.query(params);
            SolrDocumentList results = queryResponse.getResults();
            boolean empty = results.isEmpty();
            log.info("Index is empty: {}", empty);
            return empty;
        }

        private void initImport(Collection<SolrDataHandler> importHandlers) throws Exception {
            for (SolrDataHandler dataHandler : importHandlers) {
                if (solr instanceof HttpSolrServer) {
                    ((HttpSolrServer) solr).add(dataHandler.getData());
                } else {
                    for (Iterator<SolrInputDocument> iter = dataHandler.getData(); iter.hasNext(); ) {
                        SolrInputDocument doc = iter.next();
                        solr.add(doc);
                    }
                }
                solr.commit();
            }
        }


        /**
         * Rebuild index for all documents
         */
        private void rebuildIndex(Collection<SolrDataHandler> importHandlers) throws Exception {
            log.info("Rebuilding SORL index");
            solr.deleteByQuery("*:*");
            solr.commit();
            initImport(importHandlers);
        }
    }
}

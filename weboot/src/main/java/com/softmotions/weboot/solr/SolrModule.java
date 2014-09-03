package com.softmotions.weboot.solr;

import com.softmotions.weboot.WBConfiguration;

import com.google.inject.AbstractModule;

import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public class SolrModule extends AbstractModule {

    private WBConfiguration cfg;

    private Collection<SolrImportHandler> importHandlers;

    public SolrModule(WBConfiguration cfg) {
        this.cfg = cfg;
        this.importHandlers = new ArrayList<>();
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

            // TODO: check
            String providerClassName = scfg.getString("provider[@class]");
            Class<?> providerClass = cl.loadClass(providerClassName);
            if (!SolrServerProvider.class.isAssignableFrom(providerClass)) {
                // TODO: error
                throw new RuntimeException("provider class");
            }
            SolrServerProvider ssp = ((Class<? extends SolrServerProvider>) providerClass).newInstance();
            SolrServer solr = ssp.get(scfg.configurationAt("provider"));

            for (HierarchicalConfiguration dhcfg : scfg.configurationsAt("data-handlers.data-handler")) {
                try {
                    String dhClassName = dhcfg.getString("[@class]");
                    Class<?> dhClass = cl.loadClass(dhClassName);

                    // TODO: check
                    if (!SolrImportHandler.class.isAssignableFrom(dhClass)) {
                        throw new RuntimeException("data handler");
                    }

                    importHandlers.add(((Class<? extends SolrImportHandler>) dhClass).newInstance());
                } catch (ClassNotFoundException e) {
                    // TODO:
                }
            }

            if (scfg.getBoolean("[@rebuildIndex]", false)) {
                rebuildIndex(solr);
            }

            bind(SolrServer.class).toInstance(solr);
        } catch (Exception e) {
            System.out.println();
            System.out.println("error");
            // TODO:
        }
    }

    protected void rebuildIndex(SolrServer solr) {
        try {
            solr.deleteByQuery("*:*");
            solr.commit();

            for (SolrImportHandler importHandler : importHandlers) {
//                UpdateRequest req = new UpdateRequest();
//                req.setAction(UpdateRequest.ACTION.COMMIT, false, false);
//
//                if (solr instanceof HttpSolrServer) {
//                    ((HttpSolrServer) solr).add(importHandler.getData());
//                } else {
                for (Iterator<SolrInputDocument> iter = importHandler.getData(); iter.hasNext(); ) {
                    SolrInputDocument doc = iter.next();
//                    req.add(doc);
                    solr.add(doc);
                }
//                }
                solr.commit();
//                UpdateResponse rsp = req.process(solr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

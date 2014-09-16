package com.softmotions.weboot.solr;

import com.softmotions.weboot.WBConfiguration;

import org.apache.commons.configuration.Configuration;
import org.apache.solr.client.solrj.SolrServer;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public abstract class AbstractSolrServerProvider implements Provider<SolrServer> {

    protected final Configuration cfg;

    @Inject
    protected AbstractSolrServerProvider(WBConfiguration cfg) {
        this.cfg = cfg.impl().configurationAt("solr.provider");
    }
}

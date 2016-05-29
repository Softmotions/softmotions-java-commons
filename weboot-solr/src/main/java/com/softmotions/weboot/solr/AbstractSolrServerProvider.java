package com.softmotions.weboot.solr;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.configuration2.Configuration;
import org.apache.solr.client.solrj.SolrServer;

import com.softmotions.weboot.WBConfiguration;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public abstract class AbstractSolrServerProvider implements Provider<SolrServer> {

    protected final WBConfiguration cfg;

    protected final Configuration scfg;

    @Inject
    protected AbstractSolrServerProvider(WBConfiguration cfg) {
        this.cfg = cfg;
        this.scfg = cfg.xcfg().configurationAt("solr.provider");
    }
}

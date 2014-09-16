package com.softmotions.weboot.solr;

import com.softmotions.weboot.WBConfiguration;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import javax.inject.Inject;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public class HttpSolrServerProvider extends AbstractSolrServerProvider {

    @Inject
    public HttpSolrServerProvider(WBConfiguration cfg) {
        super(cfg);
    }

    public SolrServer get() {
//        TODO: check & configure
        return new HttpSolrServer(scfg.getString("connectionUrl"));
    }
}

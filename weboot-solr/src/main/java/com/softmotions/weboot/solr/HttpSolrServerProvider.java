package com.softmotions.weboot.solr;

import javax.inject.Inject;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.softmotions.weboot.WBConfiguration;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public class HttpSolrServerProvider extends AbstractSolrServerProvider {

    @Inject
    public HttpSolrServerProvider(WBConfiguration cfg) {
        super(cfg);
    }

    @Override
    public SolrServer get() {
//        TODO: check & configure
        return new HttpSolrServer(scfg.getString("connectionUrl"));
    }
}

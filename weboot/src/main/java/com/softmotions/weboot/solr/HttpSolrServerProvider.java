package com.softmotions.weboot.solr;

import org.apache.commons.configuration.Configuration;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public class HttpSolrServerProvider implements SolrServerProvider {

    public SolrServer get(Configuration cfg) {
//        TODO: check & configure
        return new HttpSolrServer(cfg.getString("connectionUrl"));
    }
}

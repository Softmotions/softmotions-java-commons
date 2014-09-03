package com.softmotions.weboot.solr;

import org.apache.commons.configuration.Configuration;
import org.apache.solr.client.solrj.SolrServer;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public interface SolrServerProvider {

    SolrServer get(Configuration cfg);

}

package com.softmotions.weboot.solr;

import org.apache.commons.configuration2.Configuration;
import org.apache.solr.common.SolrInputDocument;

import java.util.Iterator;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public interface SolrDataHandler {

    void init(Configuration cfg);

    Iterator<SolrInputDocument> getData();
}

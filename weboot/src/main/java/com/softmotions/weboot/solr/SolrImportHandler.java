package com.softmotions.weboot.solr;

import org.apache.solr.common.SolrInputDocument;

import java.util.Iterator;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public interface SolrImportHandler {

    void init();

    Iterator<SolrInputDocument> getData();
}

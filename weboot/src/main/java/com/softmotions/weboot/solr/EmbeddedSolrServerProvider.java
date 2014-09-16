package com.softmotions.weboot.solr;

import com.softmotions.commons.io.Loader;
import com.softmotions.weboot.WBConfiguration;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.ConfigSet;
import org.apache.solr.core.ConfigSolr;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.xml.sax.InputSource;

import javax.inject.Inject;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
public class EmbeddedSolrServerProvider extends AbstractSolrServerProvider {

    @Inject
    public EmbeddedSolrServerProvider(WBConfiguration cfg) {
        super(cfg);
    }

    public SolrServer get() {
        String coreName = cfg.getString("[@name]");
        if (StringUtils.isBlank(coreName)) {
            throw new RuntimeException("Missing required '@name' parameter for EmbeddedSolrServerProvider");
        }
        String solrHome = cfg.getString("instance-dir");
        if (StringUtils.isBlank(solrHome)) {
            throw new RuntimeException("Missing required 'instance-dir' parameter for EmbeddedSolrServerProvider");
        }
        // TODO: substitute {home}|{tmp}|...
        System.setProperty("solr.solr.home", solrHome);

        SolrResourceLoader solrResourceLoader = new SolrResourceLoader(solrHome);

        String solrConfigFile = cfg.getString("config");
        if (StringUtils.isBlank(solrConfigFile)) {
            throw new RuntimeException("Missing required 'config' parameter for EmbeddedSolrServerProvider");
        }
        ConfigSolr solrConfig = null;
        try {
            solrConfig = ConfigSolr.fromInputStream(solrResourceLoader, Loader.getResourceAsUrl(solrConfigFile, getClass()).openStream());
        } catch (Exception e) {
            throw new RuntimeException("Error loading solr core config: " + e.getLocalizedMessage(), e);
        }

        CoreContainer coreContainer = new CoreContainer(solrResourceLoader, solrConfig);
        coreContainer.load();

        String coreConfigFile = cfg.getString("core-config");
        if (StringUtils.isBlank(coreConfigFile)) {
            throw new RuntimeException("Missing required 'core-config' parameter for EmbeddedSolrServerProvider");
        }
        SolrConfig coreConfig = null;
        try {
            coreConfig = new SolrConfig(solrHome, coreName, new InputSource(Loader.getResourceAsUrl(coreConfigFile, getClass()).openStream()));
        } catch (Exception e) {
            throw new RuntimeException("Error loading solr core config: " + e.getLocalizedMessage(), e);
        }

        String coreSchemaFile = cfg.getString("core-schema");
        if (StringUtils.isBlank(coreSchemaFile)) {
            throw new RuntimeException("Missing required 'core-schema' parameter for EmbeddedSolrServerProvider");
        }
        IndexSchema coreSchema = null;
        try {
            coreSchema = new IndexSchema(coreConfig, coreName, new InputSource(Loader.getResourceAsUrl(coreSchemaFile, getClass()).openStream()));
        } catch (Exception e) {
            throw new RuntimeException("Error loading solr core shema: " + e.getLocalizedMessage(), e);
        }

        CoreDescriptor cd = new CoreDescriptor(coreContainer, coreName, solrHome);
        ConfigSet configSet = new ConfigSet(coreName, coreConfig, coreSchema);
        SolrCore solrCore = new SolrCore(cd, configSet);
        coreContainer.register(solrCore, false);

        return new EmbeddedSolrServer(coreContainer, coreName);
    }
}

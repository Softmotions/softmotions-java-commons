package com.softmotions.weboot.solr;

import java.io.InputStream;
import javax.inject.Inject;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.google.inject.Singleton;
import com.softmotions.commons.io.Loader;
import com.softmotions.weboot.WBConfiguration;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
@Singleton
public class EmbeddedSolrServerProvider extends AbstractSolrServerProvider {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedSolrServerProvider.class);

    @Inject
    public EmbeddedSolrServerProvider(WBConfiguration cfg) {
        super(cfg);
    }

    @Override
    public SolrServer get() {

        String coreName = scfg.getString("name");
        if (StringUtils.isBlank(coreName)) {
            throw new RuntimeException("Missing required '@name' parameter for EmbeddedSolrServerProvider");
        }
        String solrHome = scfg.getString("instance-dir");
        if (StringUtils.isBlank(solrHome)) {
            throw new RuntimeException("Missing required 'instance-dir' parameter for EmbeddedSolrServerProvider");
        }
        String solrConfigFile = scfg.getString("config");
        if (StringUtils.isBlank(solrConfigFile)) {
            throw new RuntimeException("Missing required 'config' parameter for EmbeddedSolrServerProvider");
        }
        String coreConfigFile = scfg.getString("core-config");
        if (StringUtils.isBlank(coreConfigFile)) {
            throw new RuntimeException("Missing required 'core-config' parameter for EmbeddedSolrServerProvider");
        }
        String coreSchemaFile = scfg.getString("core-schema");
        if (StringUtils.isBlank(coreSchemaFile)) {
            throw new RuntimeException("Missing required 'core-schema' parameter for EmbeddedSolrServerProvider");
        }

        log.info("SOLR" +
                 "\n\tHOME: " + solrHome +
                 "\n\tCORE NAME: " + coreName +
                 "\n\tCONFIG: " + solrConfigFile +
                 "\n\tCORE CONFIG: " + coreConfigFile +
                 "\n\tSCHEMA: " + coreSchemaFile);

        System.setProperty("solr.solr.home", solrHome);

        ConfigSolr solrConfig;
        SolrConfig coreConfig;
        IndexSchema coreSchema;

        SolrResourceLoader solrResourceLoader = new SolrResourceLoader(solrHome);
        try (InputStream is = Loader.getResourceAsUrl(solrConfigFile, getClass()).openStream()) {
            solrConfig = ConfigSolr.fromInputStream(solrResourceLoader, is);
        } catch (Exception e) {
            throw new RuntimeException("Error loading solr core config: " + e.getLocalizedMessage(), e);
        }

        CoreContainer coreContainer = new CoreContainer(solrResourceLoader, solrConfig);
        coreContainer.load();
        try (InputStream is = Loader.getResourceAsUrl(coreConfigFile, getClass()).openStream()) {
            coreConfig = new SolrConfig(solrHome, coreName, new InputSource(is));
        } catch (Exception e) {
            throw new RuntimeException("Error loading solr core config: " + e.getLocalizedMessage(), e);
        }
        try (InputStream is = Loader.getResourceAsUrl(coreSchemaFile, getClass()).openStream()) {
            coreSchema = new IndexSchema(coreConfig, coreName, new InputSource(is));
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

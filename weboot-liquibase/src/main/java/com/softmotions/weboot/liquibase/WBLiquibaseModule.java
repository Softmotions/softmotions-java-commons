package com.softmotions.weboot.liquibase;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.ContextExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import com.softmotions.commons.ServicesConfiguration;

/**
 * Liquibase Guice integration.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBLiquibaseModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(WBLiquibaseModule.class);

    private final ServicesConfiguration cfg;

    public WBLiquibaseModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {
        if (cfg.xcfg().configurationsAt("liquibase").isEmpty()) {
            log.warn("No WBLiquibaseModule module configuration found. Skipping.");
            return;
        }
        Multibinder.newSetBinder(binder(), WBLiquibaseExtraConfigSupplier.class);
        binder().requestInjection(new LiquibaseInitializer());
    }

    public static class LiquibaseInitializer {

        @Inject
        public void start(DataSource ds,
                          ServicesConfiguration cfg,
                          Set<WBLiquibaseExtraConfigSupplier> extraConfigSuppliers) throws Exception {
            HierarchicalConfiguration<ImmutableNode> xcfg = cfg.xcfg();
            if (xcfg.configurationsAt("liquibase").isEmpty()) {
                log.warn("No <liquibase> configuration found");
                return;
            }
            HierarchicalConfiguration<ImmutableNode> lbCfg = xcfg.configurationAt("liquibase");
            String changelogResource = lbCfg.getString("changelog");
            if (changelogResource == null) {
                throw new RuntimeException("Missing required attribute 'changelog' in <liquibase> configuration tag");
            }
            log.info("Using changelog: {}", changelogResource);

            try (Connection connection = ds.getConnection()) {
                Database database = DatabaseFactory.getInstance()
                                                   .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName(lbCfg.getString("defaultSchema"));
                ResourceAccessor resourceAccessor = new CompositeResourceAccessor(
                        new ClassLoaderResourceAccessor(),
                        new FileSystemResourceAccessor(),
                        new ClassLoaderResourceAccessor(Thread.currentThread()
                                                              .getContextClassLoader())
                );
                ChangeLogParser parser =
                        ChangeLogParserFactory.getInstance()
                                              .getParser(changelogResource, resourceAccessor);
                ChangeLogParameters changeLogParameters = new ChangeLogParameters(database);
                DatabaseChangeLog changeLog = parser.parse(changelogResource, changeLogParameters, resourceAccessor);
                for (WBLiquibaseExtraConfigSupplier ecs : extraConfigSuppliers) {
                    for (WBLiquibaseExtraConfigSupplier.ConfigSpec cs : ecs.getConfigSpecs()) {
                        log.info("Include extra liquibase file: {} context: {}",
                                 cs.getLocation(), StringUtils.trimToEmpty(cs.getIncludeContexts()));
                        changeLog.include(cs.getLocation(), false, resourceAccessor,
                                          new ContextExpression(cs.getIncludeContexts()), true);
                    }
                }

                Liquibase liquibase =
                        new Liquibase(changeLog,
                                      resourceAccessor,
                                      database
                        );

                List<HierarchicalConfiguration<ImmutableNode>> hcList =
                        lbCfg.configurationsAt("liquibase.changelog-parameters.parameter");
                for (final HierarchicalConfiguration<ImmutableNode> hc : hcList) {
                    String name = hc.getString("name");
                    String value = hc.getString("value");
                    if (name != null) {
                        liquibase.setChangeLogParameter(name, value);
                    }
                }

                // DropAll staff
                if (lbCfg.containsKey("update.dropAll") || lbCfg.containsKey("update.dropAll.activate")) {
                    boolean activate = BooleanUtils.toBoolean(lbCfg.getString("update.dropAll.activate", "true"));
                    if (activate) {
                        String bsql = lbCfg.getString("update.dropAll.sql-before", null);
                        if (bsql != null) {
                            boolean failOnError = lbCfg.getBoolean("update.dropAll.sql-before[@failOnError]", true);
                            log.info("Executing before dropall sql. FailOnError={}", failOnError);
                            for (String sql : bsql.split(";")) {
                                sql = sql.trim();
                                if (sql.isEmpty()) {
                                    continue;
                                }
                                log.info("{};", sql);
                                try (Connection conn = ds.getConnection()) {
                                    try (Statement stmt = conn.createStatement()) {
                                        stmt.execute(sql);
                                    }
                                    conn.commit();
                                } catch (Exception e) {
                                    if (failOnError) {
                                        throw e;
                                    } else {
                                        log.warn("Sql failed: {} error: {}", sql, e.toString());
                                    }
                                }
                            }
                        }
                        log.info("Executing Liqubase.DropAll");
                        liquibase.dropAll();
                    }
                }

                // Update
                if (lbCfg.containsKey("update.contexts")) {
                    String contexts = lbCfg.getString("update.contexts");
                    log.info("Executing Liquibase update, contexts={}", contexts);
                    liquibase.update(contexts);
                } else if (lbCfg.containsKey("update")) {
                    log.info("Executing Liquibase update");
                    liquibase.update("");
                }
            }
        }
    }
}

package com.softmotions.weboot.liquibase;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Start;

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
        bind(LiquibaseInitializer.class).asEagerSingleton();
    }

    public static class LiquibaseInitializer {

        @Inject
        DataSource ds;

        @Inject
        ServicesConfiguration cfg;

        @Start(order = 10)
        public void start() throws Exception {
            HierarchicalConfiguration<ImmutableNode> xcfg = cfg.xcfg();
            HierarchicalConfiguration<ImmutableNode> lbCfg = xcfg.configurationAt("liquibase");
            if (lbCfg == null) {
                log.warn("No <liquibase> configuration found");
                return;
            }
            String changelogResource = lbCfg.getString("changelog");
            if (changelogResource == null) {
                throw new RuntimeException("Missing required attribute 'changelog' in <liquibase> configuration tag");
            }
            log.info("Using changelog: {}", changelogResource);

            try (Connection connection = ds.getConnection()) {
                Database database = DatabaseFactory.getInstance()
                                                   .findCorrectDatabaseImplementation(new JdbcConnection(connection));
                database.setDefaultSchemaName(lbCfg.getString("defaultSchema"));
                Liquibase liquibase =
                        new Liquibase(changelogResource,
                                      new CompositeResourceAccessor(
                                              new ClassLoaderResourceAccessor(),
                                              new FileSystemResourceAccessor(),
                                              new ClassLoaderResourceAccessor(Thread.currentThread()
                                                                                    .getContextClassLoader())
                                      ),
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

            } catch (Exception e) {
                log.error("Failed to initiate WBLiquibaseModule", e);
                throw new RuntimeException(e);
            }
        }
    }

}

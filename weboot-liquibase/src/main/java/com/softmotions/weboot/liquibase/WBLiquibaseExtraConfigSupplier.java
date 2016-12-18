package com.softmotions.weboot.liquibase;

import com.google.common.base.MoreObjects;

/**
 * Liquibase extra configs supplier, contained
 * in Guice multibinding Set.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WBLiquibaseExtraConfigSupplier {

    /**
     * Extra liquibase configurations
     */
    ConfigSpec[] getConfigSpecs();

    /**
     * Configuration spec.
     */
    class ConfigSpec {

        private String location;

        private String includeContexts;

        public String getLocation() {
            return location;
        }

        public String getIncludeContexts() {
            return includeContexts;
        }

        public ConfigSpec(String location) {
            this.location = location;
        }

        public ConfigSpec(String location, String includeContexts) {
            this.location = location;
            this.includeContexts = includeContexts;
        }

        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("location", location)
                              .add("includeContexts", includeContexts)
                              .toString();
        }
    }
}

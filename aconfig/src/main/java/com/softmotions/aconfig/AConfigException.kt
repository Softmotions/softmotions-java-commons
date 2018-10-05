package com.softmotions.aconfig

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class AConfigException(msg: String) : RuntimeException(msg) {

    companion object {

        fun throwMissingRequiredConfigurationParameterer(name: String): Nothing {
            throw AConfigException("Missing required configuration parameter: ${name}")
        }

        fun throwConfigurationSubstitutorCannotbetSetForWritableConfig(): Nothing {
            throw AConfigException("Configuration substitutor cannot be set for writable config")
        }

        fun throwReadOnlyConfiguration(): Nothing {
            throw AConfigException("Read-only configuration")
        }

        fun throwMatchedMoreThanOneElement(): Nothing {
            throw AConfigException("Matched more than one configuration element")
        }
    }
}
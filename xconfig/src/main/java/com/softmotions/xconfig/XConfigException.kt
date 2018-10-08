package com.softmotions.xconfig

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class XConfigException(msg: String) : RuntimeException(msg) {

    companion object {

        fun throwMissingParameter(name: String): Nothing {
            throw XConfigException("Missing required configuration parameter: ${name}")
        }

        fun throwMissingParameter(xcfg: XConfig, name: String): Nothing {
            // todo
            throw XConfigException("Missing required configuration parameter: ${name}")
        }

        fun throwConfigurationSubstitutorCannotbetSetForWritableConfig(): Nothing {
            throw XConfigException("Configuration substitutor cannot be set for writable config")
        }

        fun throwReadOnlyConfiguration(): Nothing {
            throw XConfigException("Read-only configuration")
        }

        fun throwMatchedMoreThanOneElement(): Nothing {
            throw XConfigException("Matched more than one configuration element")
        }
    }
}
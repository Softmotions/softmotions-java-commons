package com.softmotions.weboot.repository

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.weboot.repository.fs.FSRepository
import com.softmotions.weboot.repository.s3.AWS3Repository

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class WBRepositoryModule(private val env: ServicesConfiguration) : AbstractModule() {
    override fun configure() {

        env.xcfg().subPattern("repository.fs").forEach { cfg ->
            val name = cfg.text("name")
            if (name != null) {
                bind(WBRepository::class.java).annotatedWith(Names.named(name)).toInstance(FSRepository(cfg))
            } else {
                bind(WBRepository::class.java).toInstance(FSRepository(cfg))
            }
        }

        env.xcfg().subPattern("repository.s3").forEach { cfg ->
            val name = cfg.text("name")
            if (name != null) {
                bind(WBRepository::class.java).annotatedWith(Names.named(name)).toInstance(AWS3Repository(cfg))
            } else {
                bind(WBRepository::class.java).toInstance(AWS3Repository(cfg))
            }
        }
    }
}
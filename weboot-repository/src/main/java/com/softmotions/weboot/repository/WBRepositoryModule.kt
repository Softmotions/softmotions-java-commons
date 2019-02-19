package com.softmotions.weboot.repository

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.weboot.repository.fs.FileSystemFileRepository
import com.softmotions.weboot.repository.s3.AWSS3Repository
import com.softmotions.xconfig.XConfig

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class WBRepositoryModule(private val env: ServicesConfiguration) : AbstractModule() {
    override fun configure() {
        val xcfg = env.xcfg()

        fun getRepoConfig(name: String): XConfig? {
            val repoCfg = xcfg.subPattern(name)
            if (repoCfg.size > 1) {
                throw Exception("Multiple '$name' repository configurations are not allowed")
            }
            return repoCfg.firstOrNull()
        }

        var cfg = getRepoConfig("repository.fs")
        if (cfg != null) {
            val name = cfg.text("name")
            if (name != null) {
                bind(WBRepository::class.java).annotatedWith(Names.named(name)).to(FileSystemFileRepository::class.java)
            } else {
                bind(WBRepository::class.java).to(FileSystemFileRepository::class.java)
            }
        }
        cfg = getRepoConfig("repository.s3")
        if (cfg != null) {
            val name = cfg.text("name")
            if (name != null) {
                bind(WBRepository::class.java).annotatedWith(Names.named(name)).to(AWSS3Repository::class.java)
            } else {
                bind(WBRepository::class.java).to(AWSS3Repository::class.java)
            }
        }
    }
}
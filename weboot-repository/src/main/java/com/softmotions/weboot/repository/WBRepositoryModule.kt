package com.softmotions.weboot.repository

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import com.google.inject.name.Named
import com.google.inject.name.Names
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.weboot.repository.fs.FileSystemFileRepository
import com.softmotions.weboot.repository.s3.AWSS3Repository

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class WBRepositoryModule(private val env: ServicesConfiguration) : AbstractModule() {
    override fun configure() {
        val xcfg = env.xcfg()
        if (xcfg.hasPattern("repository.fs")) {
            val name = xcfg.text("repository.fs.name")
            if (name != null) {
                bind(WBRepository::class.java).annotatedWith(Names.named(name)).to(FileSystemFileRepository::class.java)
            } else {
                bind(WBRepository::class.java).to(FileSystemFileRepository::class.java)
            }
        }
        if (xcfg.hasPattern("repository.s3")) {
            val name = xcfg.text("repository.s3.name")
            if (name != null) {
                bind(WBRepository::class.java).annotatedWith(Names.named(name)).to(AWSS3Repository::class.java)
            } else {
                bind(WBRepository::class.java).to(AWSS3Repository::class.java)
            }
        }
    }
}
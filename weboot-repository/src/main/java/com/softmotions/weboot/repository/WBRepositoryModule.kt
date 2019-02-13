package com.softmotions.weboot.repository

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.weboot.repository.fs.FileSystemFileRepository
import com.softmotions.weboot.repository.s3.AWSS3Repository

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class WBRepositoryModule(private val env: ServicesConfiguration) : AbstractModule() {

    override fun configure() {
        val xcfg = env.xcfg()
        if (xcfg.hasPattern("repository.fs.root")) {
            bind(WBRepository::class.java).to(FileSystemFileRepository::class.java).`in`(Singleton::class.java)
        } else if (xcfg.hasPattern("repository.s3.bucket")
                && xcfg.hasPattern("repository.s3.region")) {
            bind(WBRepository::class.java).to(AWSS3Repository::class.java).`in`(Singleton::class.java)
        }
    }
}
package com.softmotions.weboot.repository

import com.google.inject.AbstractModule
import com.google.inject.Singleton
import com.softmotions.commons.ServicesConfiguration
import com.softmotions.weboot.repository.fs.FileSystemFileRepository

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class WBRepositoryModule(private val env: ServicesConfiguration) : AbstractModule() {

    override fun configure() {
        if (env.xcfg().hasPattern("repository.fs.root")) {
            bind(WBRepository::class.java).to(FileSystemFileRepository::class.java).`in`(Singleton::class.java)
        }
    }
}
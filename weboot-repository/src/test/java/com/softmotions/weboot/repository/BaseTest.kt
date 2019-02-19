package com.softmotions.weboot.repository

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.name.Names

open class BaseTest {
    protected lateinit var injector: Injector

    protected lateinit var env: TestEnv

    protected fun getRepositoryByName(name: String): WBRepository {
        return injector.getInstance(Key.get(WBRepository::class.java, Names.named(name)))
    }
}
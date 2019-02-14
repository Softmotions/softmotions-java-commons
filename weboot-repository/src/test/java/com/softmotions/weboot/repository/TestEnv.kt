package com.softmotions.weboot.repository

import com.google.inject.Binder
import com.softmotions.commons.ServicesConfiguration

class TestEnv(location: String) : ServicesConfiguration(location) {
    override fun configure(binder: Binder) {
        super.configure(binder)
        binder.bind(TestEnv::class.java).toInstance(this)
        binder.install(WBRepositoryModule(this))
    }
}
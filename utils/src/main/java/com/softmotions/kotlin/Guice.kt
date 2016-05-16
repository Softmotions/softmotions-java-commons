package com.softmotions.kotlin

import com.google.inject.AbstractModule
import com.google.inject.Injector

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

inline fun <reified T : Any> Injector.get(): T {
    return this.getInstance(T::class.java)
}

class InstancesModule(vararg val ilist: Any) : AbstractModule() {
    override fun configure() {
        for (i in ilist) {
            bind(i.javaClass)!!.toInstance(i)
        }
    }
}
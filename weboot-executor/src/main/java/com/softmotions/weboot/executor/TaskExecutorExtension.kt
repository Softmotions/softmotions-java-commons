package com.softmotions.weboot.executor

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.name.Names
import com.softmotions.commons.JVMResources
import java.util.concurrent.Future

/**
 * Kotlin executor
 *
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public fun executor(name: String? = null, block: () -> Unit): Future<*> {
    val injector = JVMResources.getOrFail<Injector>("com.softmotions.weboot.WBServletListener.Injector")
    val executor = if (name == null) {
        injector.getInstance(TaskExecutor::class.java)
    } else {
        injector.getInstance(Key.get(TaskExecutor::class.java, Names.named(name)))
    }
    return executor.submit(block)
}

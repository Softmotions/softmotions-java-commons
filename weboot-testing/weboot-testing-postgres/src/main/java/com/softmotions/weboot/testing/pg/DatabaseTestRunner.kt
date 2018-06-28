package com.softmotions.weboot.testing.pg

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
interface DatabaseTestRunner {

    fun setupDb(props: Map<String, Any> = emptyMap())

    fun shutdownDb()
}
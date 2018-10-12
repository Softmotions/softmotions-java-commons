package com.softmotions.xconfig

import org.jetbrains.annotations.Contract
import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.Writer
import java.net.URI
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Yet another configuration
 *
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
interface XConfig {

    val uri: URI

    val document: Document

    val parent: XConfig?

    val node: Element

    val lock: ReentrantReadWriteLock

    /**
     * List of child configs for wich
     * this config acts as master
     */
    val slaves: List<XConfig>

    fun throwMissing(name: String): Nothing

    @Contract("_, true -> !null")
    operator fun get(expr: String, require: Boolean = false, type: XCPath = XCPath.PATTERN): String?

    operator fun <T> set(expr: String, v: T)

    fun <T> setPattern(expr: String, v: T)

    fun setAttrs(expr: String, vararg pairs: Pair<String, Any>): Int

    fun setAttrsXPath(expr: String, vararg pairs: Pair<String, Any>): Int

    fun attr(name: String, value: String): Attr

    fun sub(expr: String, type: XCPath = XCPath.PATTERN): List<XConfig>

    fun subPattern(expr: String): List<XConfig>

    fun subXPath(expr: String): List<XConfig>

    fun sub(el: Element): XConfig

    fun has(expr: String, type: XCPath = XCPath.PATTERN): Boolean

    fun hasPattern(expr: String): Boolean

    fun nodes(expr: String, type: XCPath = XCPath.PATTERN): List<Node>

    fun nodesXPath(expr: String): List<Node>

    fun nodesPattern(expr: String): List<Node>

    fun detach(expr: String, type: XCPath = XCPath.PATTERN)

    fun detachXPath(expr: String)

    fun detachPattern(expr: String)

    @Contract("_, !null -> !null")
    fun text(expr: String, dval: String? = null, type: XCPath = XCPath.PATTERN): String?

    fun text(expr: String): String?

    @Contract("_, !null -> !null")
    fun textXPath(expr: String, dval: String? = null): String?

    @Contract("_, !null -> !null")
    fun textPattern(expr: String, dval: String? = null): String?

    fun list(expr: String, type: XCPath = XCPath.PATTERN): List<String>

    fun listPattern(expr: String): List<String>

    fun listXPath(expr: String): List<String>

    @Contract("_, !null -> !null")
    fun bool(expr: String, dval: Boolean? = null, type: XCPath = XCPath.PATTERN): Boolean

    fun bool(expr: String): Boolean

    @Contract("_, !null -> !null")
    fun boolXPath(expr: String, dval: Boolean? = false): Boolean

    @Contract("_, !null -> !null")
    fun boolPattern(expr: String, dval: Boolean? = false): Boolean

    @Contract("_, !null -> !null")
    fun number(expr: String, dval: Long? = null, type: XCPath = XCPath.PATTERN): Long?

    fun number(expr: String): Long?

    @Contract("_, !null -> !null")
    fun numberXPath(expr: String, dval: Long? = null): Long?

    @Contract("_, !null -> !null")
    fun numberPattern(expr: String, dval: Long? = null): Long?

    @Contract("_, !null -> !null")
    fun int(expr: String, dval: Int? = null, type: XCPath = XCPath.PATTERN): Int?

    fun int(expr: String): Int?

    @Contract("_, !null -> !null")
    fun intXPath(expr: String, dval: Int? = null): Int?

    @Contract("_, !null -> !null")
    fun intPattern(expr: String, dval: Int? = null): Int?

    fun <T> batch(action: (cfg: XConfig) -> T): T

    fun writeTo(out: Writer)

    fun save()
}
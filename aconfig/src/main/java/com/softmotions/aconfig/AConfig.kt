package com.softmotions.aconfig

import org.w3c.dom.Attr
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.net.URI

/**
 * Yet another configuration
 *
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
interface AConfig {

    val uri: URI

    val document: Document

    val parent: AConfig?

    operator fun get(expr: String, type: ACPath = ACPath.PATTERN): String?

    operator fun <T> set(expr: String, v: T)

    fun setAttrs(expr: String, vararg pairs: Pair<String, Any>): Int

    fun setAttrsXPath(expr: String, vararg pairs: Pair<String, Any>): Int

    fun attr(name: String, value: String): Attr

    fun sub(expr: String, type: ACPath = ACPath.PATTERN): List<AConfig>

    fun has(expr: String, type: ACPath = ACPath.PATTERN): Boolean

    fun nodes(expr: String, type: ACPath = ACPath.PATTERN): List<Node>

    fun nodesXPath(expr: String): List<Node>

    fun detach(expr: String, type: ACPath = ACPath.PATTERN)

    fun detachXPath(expr: String)

    fun text(expr: String, dval: String? = null, type: ACPath = ACPath.PATTERN): String?

    fun textXPath(expr: String, dval: String? = null): String?

    fun bool(expr: String, dval: Boolean = false, type: ACPath = ACPath.PATTERN): Boolean

    fun boolXPath(expr: String, dval: Boolean = false): Boolean

    fun long(expr: String, dval: Long? = null, type: ACPath = ACPath.PATTERN): Long?

    fun longXPath(expr: String, dval: Long? = null): Long?

    fun <T> batch(action: (cfg: AConfig) -> T): T

    fun save()
}
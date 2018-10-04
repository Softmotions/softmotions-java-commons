package com.softmotions.aconfig

import org.w3c.dom.*
import org.xml.sax.InputSource
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.concurrent.withLock


inline fun Node.firstChildElement(predicate: (n: Element) -> Boolean): Element? {
    val cn = childNodes
    (0 until cn.length).forEach { idx ->
        val item = cn.item(idx)
        if (item is Element && predicate(item)) {
            return item
        }
    }
    return null
}

fun <T> Element.set(v: T) {
    when (v) {
        is Attribute ->
            if (v.value != null) {
                this.setAttribute(v.name, v.value.toString())
            } else {
                this.removeAttribute(v.name)
            }
        is Attr ->
            this.setAttribute(v.name, v.value)
        is Node ->
            this.appendChild(v)
        is Sequence<*> -> v.forEach { this.set(it) }
        is Iterable<*> -> v.forEach { this.set(it) }
        else ->
            this.textContent = v?.toString() ?: ""
    }
}

data class Attribute(val name: String, val value: Any? = null)


fun <T> AConfig.batch(action: () -> T): T {
    lock.withLock {
        val prev = autosave
        autosave = false
        try {
            return action().also {
                save()
            }
        } finally {
            autosave = prev
        }
    }
}

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class AConfig(val file: File,
              var autosave: Boolean = false) {

    val document: Document

    val lock = ReentrantLock()

    private val xpf = XPathFactory.newInstance()

    init {
        if (!file.exists()) {
            if (file.parent != null) {
                file.parentFile.mkdirs()
            }
        }
        if (!file.exists() || file.length() < 1) {
            file.writeText("""
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration>
                </configuration>
            """.trimIndent())
        }
        document = DocumentBuilderFactory.newInstance().let { f ->
            f.isNamespaceAware = true
            file.bufferedReader().use {
                f.newDocumentBuilder().parse(InputSource(it))
            }
        }
    }

    operator fun <T> set(pattern: String, v: T) = lock.withLock {
        var n = document.firstChildElement { true }!! // root
        pattern.split('.').forEach { s ->
            n = n.firstChildElement { it.nodeName == s } ?: run {
                n.appendChild(document.createElementNS(n.namespaceURI, s)) as Element
            }
        }
        n.set(v)
        if (autosave) {
            save()
        }
    }

    operator fun get(pattern: String): String? = lock.withLock {
        var n = document.firstChildElement { true }!! // root
        pattern.split('.').forEach { s ->
            n = n.firstChildElement { it.nodeName == s } ?: return null
        }
        return n.textContent
    }

    fun string(xpath: String): String = lock.withLock {
        return xpf.newXPath().evaluate(xpath, document.firstChildElement { true })
    }

    fun nodes(xpath: String): List<Node> = lock.withLock {
        val ns = xpf.newXPath().evaluate(xpath, document.firstChildElement { true }, XPathConstants.NODESET) as NodeList
        val ret = ArrayList<Node>(ns.length)
        (0 until ns.length).forEach { idx -> ret.add(ns.item(idx)) }
        ret
    }

    fun save() = lock.withLock {
        val t = TransformerFactory.newInstance().newTransformer()
        t.setOutputProperty(OutputKeys.INDENT, "yes")
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        file.outputStream().buffered().use { o ->
            t.transform(DOMSource(document), StreamResult(o))
            o.flush()
        }
    }
}
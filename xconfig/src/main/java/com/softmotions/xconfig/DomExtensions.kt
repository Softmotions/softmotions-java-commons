package com.softmotions.xconfig

import org.apache.commons.lang3.StringUtils
import org.w3c.dom.Attr
import org.w3c.dom.Element
import org.w3c.dom.Node

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

fun Node.detach(): Boolean {
    val p = this.parentNode
    if (p != null) {
        return p.removeChild(this) != null
    } else {
        return false
    }
}

fun <T> Element.set(v: T) {
    when (v) {
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

fun Node.text(): String? = when (this) {
    is Element -> StringUtils.trimToNull(textContent)
    else -> StringUtils.trimToNull(nodeValue)
}
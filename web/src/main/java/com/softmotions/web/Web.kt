package com.softmotions.web

import com.softmotions.commons.string.EscapeHelper
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

/**
 * Servlets API Kotlin extrensions
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */


/**
 * Get request cookie with specified name or `null`
 */
fun HttpServletRequest.cookie(name: String): Cookie? {
    return (this.cookies ?: emptyArray()).find { it.name == name }
}

operator fun HttpServletRequest.get(name: String): Any? {
    @Suppress("UNCHECKED_CAST")
    return this.getAttribute(name)
}

operator fun <T> HttpServletRequest.set(name: String, value: T?) {
    this.setAttribute(name, value)
}

fun Cookie.decodeValue(): String? {
    return EscapeHelper.decodeURIComponent(this.value)
}

fun Cookie.setEncodedValue(value: String) {
    this.value = EscapeHelper.encodeURLComponent(value)
}
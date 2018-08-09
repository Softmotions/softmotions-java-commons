package com.softmotions.kotlin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.StdDateFormat
import java.util.*

/**
 * Jackson kotlin extensions.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */

fun JsonNode.toISO8601Date(): Date? {
    val text = asText()
    return if (text == null || text.isEmpty()) null else StdDateFormat().parse(asText())
}

fun ObjectNode.text(path: String, default: String = ""): String {
    return this.path(path).asText(default)
}

fun ObjectNode.long(path: String, default: Long = 0): Long {
    return this.path(path).asLong(default)
}

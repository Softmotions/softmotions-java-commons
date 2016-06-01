package com.softmotions.kotlin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.util.ISO8601DateFormat
import java.util.*

/**
 * Jackson kotlin extensions.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */

fun JsonNode.toISO8601Date(): Date? {
    val text = asText()
    return if (text == null || text.isEmpty()) null else ISO8601DateFormat().parse(asText())
}

package com.softmotions.kotlin

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

val REGEXP_ASCII_WORDNUM = Regex("[\\d\\w]*")

inline fun checkAsciiWordNum(value: String, lazyMessage: () -> Any): String {
    if (!value.matches(REGEXP_ASCII_WORDNUM)) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    } else {
        return value
    }
}

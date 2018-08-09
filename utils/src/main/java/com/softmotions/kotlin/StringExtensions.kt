package com.softmotions.kotlin

import com.softmotions.commons.string.EscapeHelper
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.net.BCodec
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

fun String.toUUID(): UUID {
    return UUID.fromString(this)
}

fun String.toURLComponent(): String = EscapeHelper.encodeURLComponent(this)

fun String.toBase64(): String = Base64.encodeBase64String(toByteArray())

fun String.toBCode(): String = BCodec().encode(this)

fun String.toFile(): File = File(this)

fun String.toPath(): Path = Paths.get(this)

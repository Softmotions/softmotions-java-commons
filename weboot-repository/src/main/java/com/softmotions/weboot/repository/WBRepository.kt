package com.softmotions.weboot.repository

import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI

/**
 * Generic file storage repository.
 *
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
interface WBRepository {

    fun acceptUri(uri: URI): Boolean

    fun fetchFileName(uri: URI): String

    fun persist(input: InputStream, fname: String): URI

    fun remove(uri: URI): Boolean

    @Throws(FileNotFoundException::class)
    fun transferTo(uri: URI, output: OutputStream)
}
package com.softmotions.weboot.repository

import java.io.InputStream
import java.io.OutputStream
import java.net.URI

/**
 * Generic file storage repository.
 *
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
interface WBRepository {

    /**
     * Check uri on belong to repository
     */
    fun acceptUri(uri: URI): Boolean

    /**
     * Get filename from uri
     */
    fun fetchFileName(uri: URI): String

    /**
     * Persist input data stream to repository
     * @param fname filename, unique key
     */
    fun persist(input: InputStream, fname: String): URI

    /**
     * Remove object from repository
     */
    fun remove(uri: URI): Boolean

    /**
     * Takes an object from repository and transfer stream of data to output
     */
    fun transferTo(uri: URI, output: OutputStream)
}
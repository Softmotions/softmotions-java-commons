package com.softmotions.runner

import javax.annotation.concurrent.NotThreadSafe

/**
 * Collect process output into string buffer.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@NotThreadSafe
class BufferOutputCollector(val maxBufSize: Int = 0,
                            val lineTransform: ((String) -> String)? = null) : OutputHandler {

    private val sb: StringBuilder by lazy {
        StringBuilder()
    }

    override fun asFn(): (String) -> Unit {
        return {
            fn(it)
        }
    }

    override fun toString(): String {
        return sb.toString()
    }

    val length: Int
        get() = sb.length

    private fun fn(line: String) {
        if (maxBufSize > 0 && line.length + sb.length > maxBufSize) {
            append(line.substring(0, maxBufSize - sb.length))
        } else {
            append(line)
        }
    }

    private fun append(line: String) {
        if (lineTransform != null) {
            sb.appendln(lineTransform.invoke(line))
        } else {
            sb.appendln(line)
        }
    }
}
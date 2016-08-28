package com.softmotions.runner

import com.softmotions.commons.string.CmdArgumentsTokenizer
import org.apache.commons.collections.map.Flat3Map
import java.util.*
import javax.annotation.concurrent.NotThreadSafe

/**
 * Split command output lines into table
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@NotThreadSafe
open class TabularOutputCollector(val expectHeader: Boolean = false,
                                  val maxRowsLimit: Int = 0,
                                  val minSplitSpaces: Int = 2,
                                  val lineTransform: ((String) -> String)? = null,
                                  val useCmdArgsTokenizer: Boolean = false) : OutputHandler {

    val header: Array<String>
        get() {
            return _headers ?: emptyArray()
        }

    val rows: List<Array<String>>
        get() {
            return _rows
        }

    val size: Int
        get() {
            return _rows.size
        }

    protected val _rows = ArrayList<Array<String>>()

    protected var _headers: Array<String>? = null

    private var _headerPositions: Array<Int>? = null

    private val _ssRegexp = Regex("[ \\t]{${minSplitSpaces},}+")

    @Volatile
    private var _cachedNamedRows: List<Map<String, String>>? = null

    fun asNamedRows(): List<Map<String, String>> {
        if (_cachedNamedRows != null) {
            return _cachedNamedRows!!
        }
        if (_headers == null || _headers!!.isEmpty()) {
            return emptyList()
        }
        val header = _headers!!
        _cachedNamedRows = _rows.map {
            @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
            val res = (
                    if (header.size > 3)
                        HashMap<String, String>(header.size)
                    else Flat3Map())
                    as MutableMap<String, String>
            for ((idx, v) in it.withIndex()) {
                if (idx < header.size) {
                    res.put(header[idx], v)
                }
            }
            return@map res
        }
        return _cachedNamedRows!!
    }

    override fun asFn(): ((line: String) -> Unit) {
        return {
            lineHandlerFn(it)
        }
    }

    protected fun lineHandlerFn(line: String) {
        if (line.isBlank()) {
            return;
        }
        val tline = if (lineTransform != null) {
            lineTransform.invoke(line).trim()
        } else {
            line.trim()
        }
        if (expectHeader && _headers == null && _rows.isEmpty()) {
            splitHeader(tline)
            return
        }
        _cachedNamedRows = null
        while (maxRowsLimit > 0 && _rows.size >= maxRowsLimit) {
            _rows.removeAt(0)
        }
        _rows += splitLine(tline)
    }

    private fun splitHeader(line: String) {
        val hdrs: Array<String>
        if (useCmdArgsTokenizer) {
            hdrs = CmdArgumentsTokenizer.tokenize(line).map { it.trim() }.toTypedArray()
        } else {
            hdrs = line.split(_ssRegexp).map { it.trim() }.toTypedArray()
        }
        val hpos = ArrayList<Int>(hdrs.size)
        var idx: Int = 0
        for (h in hdrs) {
            idx = line.indexOf(h, idx)
            if (idx == -1) {
                break
            }
            hpos += idx
            idx += h.length
            if (idx >= line.length) {
                break
            }
        }
        check(hdrs.size == hpos.size)
        _headers = hdrs
        _headerPositions = hpos.toTypedArray()
    }

    private fun splitLine(line: String): Array<String> {
        val hpos = _headerPositions
        if (hpos == null || hpos.isEmpty()) {
            if (useCmdArgsTokenizer) {
                return CmdArgumentsTokenizer.tokenize(line).map { it.trim() }.toTypedArray()
            } else {
                return line.split(_ssRegexp).map { it.trim() }.toTypedArray()
            }
        }
        val res = ArrayList<String>(hpos.size)
        for ((i, hp) in hpos.withIndex()) {
            val next = if (i < hpos.size - 1) hpos[i + 1] else line.length
            res.add(line.substring(hp, next).trim())
            if (next == line.length) {
                break
            }
        }
        return res.toTypedArray()
    }
}
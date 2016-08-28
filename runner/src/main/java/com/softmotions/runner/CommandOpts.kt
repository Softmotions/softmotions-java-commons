package com.softmotions.runner

import java.util.*
import javax.annotation.concurrent.NotThreadSafe

/**
 * Command line options.
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@NotThreadSafe
open class CommandOpts(val useEqForLongOpts: Boolean = false) : Cloneable {

    companion object {
        val EMPTY = CommandOpts()
    }

    protected val _opts: MutableList<Pair<String, String>> = ArrayList()

    val asOptPairs: List<Pair<String, String>>
        get() = _opts

    val asCmdArray: Array<String>
        get() {
            return _opts.flatMap {
                val p = it
                val n = p.first
                if (n.isBlank()) {
                    return@flatMap listOf(p.second)
                }
                if (useEqForLongOpts && n.startsWith("--") && !p.second.isBlank()) {
                    return@flatMap listOf("$n=${p.second}")
                }
                if (p.second.isEmpty()) {
                    listOf(n)
                } else {
                    listOf(n, p.second)
                }
            }.toTypedArray()
        }


    operator fun plusAssign(p: Pair<String, String>) {
        addOpts(p)
    }

    operator fun plusAssign(a: String) {
        addArgs(a)
    }

    fun addArgs(vararg a: String): CommandOpts {
        a.forEach {
            addOpts("" to it)
        }
        return this
    }

    fun addOptIfMissing(p: String): CommandOpts {
        return addOptIfMissing(p to "")
    }

    fun addOptIfMissing(p: Pair<String, String>): CommandOpts {
        _opts.find {
            it.first == p.first
        } ?: addOpts(p)
        return this
    }

    fun addOptReplace(p: Pair<String, String>): CommandOpts {
        _opts.removeAll {
            it.first == p.first
        }
        _opts.add(p)
        return this
    }

    fun addOpts(vararg p: Pair<String, String>): CommandOpts {
        _opts.addAll(p)
        return this
    }

    fun addOpts(vararg p: String): CommandOpts {
        p.forEach {
            addOpts(it to "")
        }
        return this
    }

    fun addOpts(p: List<Pair<String, String>>): CommandOpts {
        _opts.addAll(p)
        return this
    }

    public override fun clone(): Any {
        val res = CommandOpts(useEqForLongOpts)
        res._opts.addAll(_opts)
        return res
    }
}
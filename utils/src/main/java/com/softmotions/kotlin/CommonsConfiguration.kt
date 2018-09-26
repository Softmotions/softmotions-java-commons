package com.softmotions.kotlin

import org.apache.commons.configuration2.HierarchicalConfiguration
import org.apache.commons.configuration2.tree.ImmutableNode
import java.util.*

fun HierarchicalConfiguration<ImmutableNode>.toMap(): MutableMap<String, String> {
    val ret = HashMap<String, String>()
    val keysIt = this.keys
    while (keysIt.hasNext()) {
        val key = keysIt.next()
        ret.put(key, this.getString(key, null))
    }
    return ret
}

inline fun HierarchicalConfiguration<ImmutableNode>.removeFirst(at: String, filter: (sub: HierarchicalConfiguration<ImmutableNode>) -> Boolean) {
    configurationsAt(at).forEachIndexed { idx, sub ->
        if (filter(sub)) {
            this.clearTree("${at}(${idx})")
            sub.clear()
            return@forEachIndexed
        }
    }
}

fun HierarchicalConfiguration<ImmutableNode>.filterByAttrs(at: String, vararg pairs: Pair<String, String>): List<HierarchicalConfiguration<ImmutableNode>> {
    return configurationsAt(at).filter { cfg ->
        pairs.all { cfg.getString("[@${it.first}]", "") == it.second }
    }
}

fun HierarchicalConfiguration<ImmutableNode>.addIfMissing(at: String, vararg pairs: Pair<String, String>) {
    filterByAttrs(at, *pairs).firstOrNull() ?: kotlin.run {
        val idx = at.lastIndexOf('.')
        addNode(if (idx != -1) at.substring(0, idx) else "",
                if (idx != -1) at.substring(idx + 1) else at,
                *pairs)
    }
}

fun HierarchicalConfiguration<ImmutableNode>.addNode(at: String, name: String, vararg pairs: Pair<String, String>) {
    addNodes(at, listOf(ImmutableNode.Builder().name(name).also { nb ->
        pairs.forEach { nb.addAttribute(it.first, it.second) }
    }.create()))
}
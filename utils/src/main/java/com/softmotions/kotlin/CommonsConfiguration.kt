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

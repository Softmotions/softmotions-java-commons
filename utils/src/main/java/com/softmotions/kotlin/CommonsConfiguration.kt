package com.softmotions.kotlin

import org.apache.commons.configuration.AbstractConfiguration

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

fun AbstractConfiguration.getBooleanKt(name: String, def: Boolean): Boolean {
    return AbstractConfigurationKtAdapter.getBoolean(this, name, def)
}

fun AbstractConfiguration.getIntKt(name: String, def: Int): Int {
    return AbstractConfigurationKtAdapter.getInt(this, name, def)
}

fun AbstractConfiguration.getLongKt(name: String, def: Long): Long {
    return AbstractConfigurationKtAdapter.getLong(this, name, def)
}

fun AbstractConfiguration.getShortKt(name: String, def: Short): Short {
    return AbstractConfigurationKtAdapter.getShort(this, name, def)
}

fun AbstractConfiguration.getByteKt(name: String, def: Byte): Byte {
    return AbstractConfigurationKtAdapter.getByte(this, name, def)
}

fun AbstractConfiguration.getFloatKt(name: String, def: Float): Float {
    return AbstractConfigurationKtAdapter.getFloat(this, name, def)
}

fun AbstractConfiguration.getDoubleKt(name: String, def: Double): Double {
    return AbstractConfigurationKtAdapter.getDouble(this, name, def)
}


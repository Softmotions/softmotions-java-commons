package com.softmotions.kotlin

import com.softmotions.commons.string.EscapeHelper
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.net.BCodec
import org.slf4j.LoggerFactory
import java.io.StringReader
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

inline fun <reified T : Any> loggerFor() = LoggerFactory.getLogger(T::class.java);

///////////////////////////////////////////////////////////////////////////
//                           String misc                                 //
///////////////////////////////////////////////////////////////////////////

fun String.toURLComponent(): String = EscapeHelper.encodeURLComponent(this)

fun String.toBase64(): String = Base64.encodeBase64String(toByteArray())

fun String.toBCode(): String = BCodec().encode(this)

///////////////////////////////////////////////////////////////////////////
//                            Time units                                 //
///////////////////////////////////////////////////////////////////////////

data class TimeSpec(val time: Long, val unit: TimeUnit = TimeUnit.MILLISECONDS) {

    companion object {
        val ZERO: TimeSpec = TimeSpec(0L, TimeUnit.MILLISECONDS)
        val MAX: TimeSpec = TimeSpec(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        var ONE_SEC: TimeSpec = TimeSpec(1, TimeUnit.SECONDS)
        var TEN_SEC: TimeSpec = TimeSpec(10, TimeUnit.SECONDS)
        val QUATER_MIN: TimeSpec = TimeSpec(15, TimeUnit.SECONDS)
        val HALF_MIN: TimeSpec = TimeSpec(30, TimeUnit.SECONDS)
        val ONE_MIN: TimeSpec = TimeSpec(1, TimeUnit.MINUTES)
        val TEN_MIN: TimeSpec = TimeSpec(10, TimeUnit.MINUTES)
        val QUATER_HOUR: TimeSpec = TimeSpec(15, TimeUnit.MINUTES)
        val HALF_HOUR: TimeSpec = TimeSpec(30, TimeUnit.MINUTES)
        val ONE_HOUR: TimeSpec = TimeSpec(1, TimeUnit.HOURS)
    }

    fun toMillis(): Long {
        return unit.toMillis(time)
    }

    fun toNanos(): Long {
        return unit.toNanos(time)
    }

    operator fun compareTo(u: Long): Int {
        return unit.toMillis(time).compareTo(u)
    }

    operator fun compareTo(u: TimeSpec): Int {
        return when (unit) {
            TimeUnit.NANOSECONDS -> unit.toNanos(time).compareTo(u.unit.toNanos(u.time))
            TimeUnit.MICROSECONDS -> unit.toMicros(time).compareTo(u.unit.toMicros(u.time))
            TimeUnit.MILLISECONDS -> unit.toMillis(time).compareTo(u.unit.toMillis(u.time))
            TimeUnit.SECONDS -> unit.toSeconds(time).compareTo(u.unit.toSeconds(u.time))
            TimeUnit.MINUTES -> unit.toMinutes(time).compareTo(u.unit.toMinutes(u.time))
            TimeUnit.HOURS -> unit.toHours(time).compareTo(u.unit.toHours(u.time))
            TimeUnit.DAYS -> unit.toDays(time).compareTo(u.unit.toDays(u.time))
        }
    }
}

fun Long.toMilliseconds(): TimeSpec = TimeSpec(this, TimeUnit.MILLISECONDS)
fun Int.toMilliseconds(): TimeSpec = TimeSpec(this.toLong(), TimeUnit.MILLISECONDS)
fun Long.toSeconds(): TimeSpec = TimeSpec(this, TimeUnit.SECONDS)
fun Int.toSeconds(): TimeSpec = TimeSpec(this.toLong(), TimeUnit.SECONDS)
fun Long.toMinutes(): TimeSpec = TimeSpec(this, TimeUnit.MINUTES)
fun Int.toMinutes(): TimeSpec = TimeSpec(this.toLong(), TimeUnit.MINUTES)
fun Long.toHours(): TimeSpec = TimeSpec(this, TimeUnit.HOURS)
fun Int.toHours(): TimeSpec = TimeSpec(this.toLong(), TimeUnit.HOURS)
fun Long.toDays(): TimeSpec = TimeSpec(this, TimeUnit.DAYS)
fun Int.toDays(): TimeSpec = TimeSpec(this.toLong(), TimeUnit.DAYS)

///////////////////////////////////////////////////////////////////////////
//                              Storage units                            //
///////////////////////////////////////////////////////////////////////////

enum class StorageUnit {

    BYTE {

        override fun toBytes(value: Long): Long {
            return value
        }

        override fun toKilobytes(value: Long): Long {
            return value / C1
        }

        override fun toMegabytes(value: Long): Long {
            return value / C2
        }

        override fun toGigabytes(value: Long): Long {
            return value / C3
        }
    },

    KILOBYTE {
        override fun toBytes(value: Long): Long {
            return value * C1
        }

        override fun toKilobytes(value: Long): Long {
            return value
        }

        override fun toMegabytes(value: Long): Long {
            return value / C1
        }

        override fun toGigabytes(value: Long): Long {
            return value / C2
        }
    },

    MEGABYTE {

        override fun toBytes(value: Long): Long {
            return value * C2
        }

        override fun toKilobytes(value: Long): Long {
            return value * C1
        }

        override fun toMegabytes(value: Long): Long {
            return value
        }

        override fun toGigabytes(value: Long): Long {
            return value / C1
        }
    },

    GIGABYTE {

        override fun toBytes(value: Long): Long {
            return value * C3
        }

        override fun toKilobytes(value: Long): Long {
            return value * C2
        }

        override fun toMegabytes(value: Long): Long {
            return value * C1
        }

        override fun toGigabytes(value: Long): Long {
            return value
        }
    };

    companion object {

        private val C0 = 0L
        private val C1 = C0 * 1024L
        private val C2 = C1 * 1024L
        private val C3 = C2 * 1024L

        fun parseLong(value: Long): StorageSpec {
            return StorageSpec(value, StorageUnit.BYTE)
        }

        fun parseString(value: String): StorageSpec {
            // 1Mb 1M 1GB 1G 1K 1Kb 1KB
            val v = value.toUpperCase()
            if (v.endsWith('M')) {
                return StorageSpec(v.substring(0, v.length - 1).toLong(), StorageUnit.MEGABYTE)
            } else if (v.endsWith('K')) {
                return StorageSpec(v.substring(0, v.length - 1).toLong(), StorageUnit.KILOBYTE)
            } else if (v.endsWith('G')) {
                return StorageSpec(v.substring(0, v.length - 1).toLong(), StorageUnit.GIGABYTE)
            }
            if (v.endsWith("MB")) {
                return StorageSpec(v.substring(0, v.length - 2).toLong(), StorageUnit.MEGABYTE)
            } else if (v.endsWith("KB")) {
                return StorageSpec(v.substring(0, v.length - 2).toLong(), StorageUnit.KILOBYTE)
            } else if (v.endsWith("GB")) {
                return StorageSpec(v.substring(0, v.length - 2).toLong(), StorageUnit.GIGABYTE)
            }
            return StorageSpec(value.toLong(), StorageUnit.BYTE)
        }
    }

    abstract fun toBytes(value: Long): Long;
    abstract fun toKilobytes(value: Long): Long;
    abstract fun toMegabytes(value: Long): Long;
    abstract fun toGigabytes(value: Long): Long;
}

data class StorageSpec(val value: Long, val unit: StorageUnit = StorageUnit.BYTE) {

    companion object {
        val ONE_BYTE = StorageSpec(1L, StorageUnit.BYTE)
        val ONE_KILOBYTE = StorageSpec(1L, StorageUnit.KILOBYTE)
        val TEN_KILOBYTES = StorageSpec(10L, StorageUnit.KILOBYTE)
        val HALF_MEGABYTE = StorageSpec(512L, StorageUnit.KILOBYTE)
        val ONE_MEGABYTE = StorageSpec(1L, StorageUnit.MEGABYTE)
        val TEN_MEGABYTES = StorageSpec(10L, StorageUnit.MEGABYTE)
        val FIFTY_MEGABYTES = StorageSpec(50L, StorageUnit.MEGABYTE)
        val HANDRED_MEGABYTES = StorageSpec(100L, StorageUnit.MEGABYTE)
        val HALF_GIGABYTE = StorageSpec(512L, StorageUnit.MEGABYTE)
        val ONE_GIGABYTE = StorageSpec(1L, StorageUnit.GIGABYTE)
    }

    fun toBytes(): Long {
        return unit.toBytes(value)
    }

    fun toKilobytes(): Long {
        return unit.toKilobytes(value)
    }

    fun toMegabytes(): Long {
        return unit.toMegabytes(value)
    }

    fun toGigabytes(): Long {
        return unit.toGigabytes(value)
    }
}

fun Long.toStorageUnitBytes(): StorageSpec = StorageSpec(this, StorageUnit.BYTE)
fun Long.toKilobytes(): StorageSpec = StorageSpec(this, StorageUnit.KILOBYTE)
fun Long.toMegabytes(): StorageSpec = StorageSpec(this, StorageUnit.MEGABYTE)
fun Long.toGigabytes(): StorageSpec = StorageSpec(this, StorageUnit.GIGABYTE)

fun Int.toStorageUnitBytes(): StorageSpec = StorageSpec(this.toLong(), StorageUnit.BYTE)
fun Int.toKilobytes(): StorageSpec = StorageSpec(this.toLong(), StorageUnit.KILOBYTE)
fun Int.toMegabytes(): StorageSpec = StorageSpec(this.toLong(), StorageUnit.MEGABYTE)
fun Int.toGigabytes(): StorageSpec = StorageSpec(this.toLong(), StorageUnit.GIGABYTE)


///////////////////////////////////////////////////////////////////////////
//                         Properties                                    //
///////////////////////////////////////////////////////////////////////////

@Suppress("UNCHECKED_CAST")
fun Properties.toMap(): Map<String, String> {
    val p = this
    val pm = this as Map<String, String>
    return object : Map<String, String> {
        override val entries: Set<Map.Entry<String, String>>
            get() = pm.entries
        override val keys: Set<String>
            get() = pm.keys
        override val size: Int
            get() = pm.size
        override val values: Collection<String>
            get() = pm.values

        override fun containsKey(key: String): Boolean {
            return pm.containsKey(key)
        }

        override fun containsValue(value: String): Boolean {
            return pm.containsValue(value)
        }

        override fun get(key: String): String? {
            return p.getProperty(key)
        }

        override fun isEmpty(): Boolean {
            return pm.isEmpty()
        }
    }
}

fun Properties.fromString(v: String): Properties {
    this.load(StringReader(v))
    return this
}

package com.softmotions.kotlin

import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

inline fun <reified T : Any> loggerFor() = LoggerFactory.getLogger(T::class.java);

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

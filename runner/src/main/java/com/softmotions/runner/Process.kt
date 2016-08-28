package com.softmotions.runner

import com.softmotions.kotlin.loggerFor
import org.slf4j.Logger
import java.lang.reflect.Field

val Process.log: Logger
    get() = loggerFor<Process>()

val Process.pid: Int
    get() {
        fun unixPid(): Int {
            val f: Field = javaClass.getDeclaredField("pid")
            f.isAccessible = true
            return f.get(this) as Int
        }

        fun win32Pid(): Int {
            TODO()
        }
        if ("java.lang.UNIXProcess" == javaClass.name) {
            return unixPid()
        } else if ("java.lang.Win32Process" == javaClass.name ||
                "java.lang.ProcessImpl" == javaClass.name) {
            return win32Pid()
        } else {
            throw UnsupportedOperationException("Unknown process implementation: ${javaClass.name}")
        }
    }

fun Process.kill(signal: UnixSignal = UnixSignal.SIGTERM): Boolean {
    if (!Platform.CURRENT.isUnixLike) {
        log.warn("Unsupported kill on non UNIX platforms, destroy will be used instead")
        destroy()
        return true
    }
    val pid = this.pid;
    val cmd = listOf<String>("kill", "-${signal.num}", "$pid")
    log.info("Killing process: ${cmd.joinToString(" ")}")
    val pb: ProcessBuilder = ProcessBuilder(cmd)
    pb.redirectErrorStream()
    val p = pb.start()
    val ecode = p.waitFor()
    return if (ecode == 0) true else false

}



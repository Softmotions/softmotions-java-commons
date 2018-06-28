package com.softmotions.runner

import com.softmotions.kotlin.loggerFor
import org.slf4j.Logger

val Process.log: Logger
    get() = loggerFor<Process>()

fun Process.kill(signal: UnixSignal = UnixSignal.SIGTERM): Boolean {
    if (!Platform.CURRENT.isUnixLike) {
        log.warn("Unsupported kill on non UNIX platforms, destroy will be used instead")
        destroy()
        return true
    }
    val pid = this.pid()
    val cmd = listOf("kill", "-${signal.num}", "$pid")
    log.info("Killing process: ${cmd.joinToString(" ")}")
    val pb = ProcessBuilder(cmd)
    pb.redirectErrorStream()
    val p = pb.start()
    val ecode = p.waitFor()
    return ecode == 0
}



package com.softmotions.runner

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
class UnixSignal(val num: Int) {

    companion object {
        // Term  Hangup detected on controlling terminal or death of controlling process
        val SIGHUP: UnixSignal = UnixSignal(1)
        // Term    Interrupt from keyboard (Ctrl-C)
        val SIGINT: UnixSignal = UnixSignal(2)
        // Term    Kill signal
        val SIGKILL: UnixSignal = UnixSignal(9)
        // Core    Invalid memory reference
        val SIGSEGV: UnixSignal = UnixSignal(11)
        // Term    Termination signal
        val SIGTERM: UnixSignal = UnixSignal(15)
    }

    operator fun compareTo(i: Int): Int {
        return num.compareTo(i)
    }

    operator fun compareTo(i: UnixSignal): Int {
        return this.compareTo(i.num)
    }
}
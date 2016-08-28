package com.softmotions.runner

import com.softmotions.kotlin.TimeSpec

/**
 * Running process interface.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
interface ProcessRun {

    /**
     * Java process instance.
     */
    val process: Process

    /**
     * Process exit code
     */
    val exitCode: Int

    /**
     * Process command line.
     */
    val command: String

    /**
     * True if process exit code is not zero
     */
    val failed: Boolean
        get() = exitCode != 0

    /**
     * Wait for this process run termination.
     */
    fun waitFor(wait: TimeSpec? = null,
                block: ((p: ProcessRun) -> Unit)? = null): Boolean

    /**
     * Halt process runner and wait for its termination.
     *
     * @param wait Max time to wait. If not specified we will wait indefinitely
     * @param signal Unix signal will be send to all of live processes.
     *               If not specified processed will be terminated by Process#destroy
     */
    fun haltRunner(wait: TimeSpec = TimeSpec.MAX,
                   signal: UnixSignal? = null,
                   block: ((p: ProcessRun) -> Any)? = null): Boolean

    /**
     * Write `line` to the process's stdin
     * @param line Line will be sent to this process
     */
    operator fun plusAssign(line: CharSequence)

    fun write(data: CharSequence, flush: Boolean = false): ProcessRun;

    fun writeln(data: CharSequence, flush: Boolean = false): ProcessRun;

    fun writeEnd(): ProcessRun;

}
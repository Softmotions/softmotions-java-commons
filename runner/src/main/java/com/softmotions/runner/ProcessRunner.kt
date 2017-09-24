package com.softmotions.runner

import com.softmotions.kotlin.TimeSpec
import java.io.Closeable
import java.io.File

//import com.softmotions.ja.run.

/**
 * Run OS process.

 * @author Adamansky Anton (adamansky@gmail.com)
 */
interface ProcessRunner : Closeable {

    val tasksNum: Int

    val userData: Map<String, Any>


    fun cmd(cmdLine: String,
            directory: File? = null,
            env: Map<String, String> = emptyMap(),
            failOnExitCode: Boolean = false,
            failOnTimeout: Boolean = false,
            stderrFn: ((line: String) -> Unit)? = null,
            outputFn: ((line: String) -> Unit)? = null): ProcessRun


    fun cmd2(cmdLine: String,
             failOnExitCode: Boolean,
             failOnTimeout: Boolean,
             outputFn: ((line: String) -> Unit)? = null): ProcessRun


    fun spec(spec: ProcessSpec,
             stdout: OutputHandler? = null,
             stderr: OutputHandler? = null): ProcessRun

    fun halt(maxWait: TimeSpec = TimeSpec.MAX,
             signal: UnixSignal? = null,
             block: ((pr: ProcessRunner) -> Unit)? = null): Boolean

    fun reset(maxWait: TimeSpec = TimeSpec.MAX,
              signal: UnixSignal? = null,
              block: ((pr: ProcessRunner) -> Unit)? = null): Boolean
}




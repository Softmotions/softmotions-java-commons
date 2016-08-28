package com.softmotions.runner

import com.softmotions.kotlin.TimeSpec
import java.io.File

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
data class ProcessSpec(val exec: String? = null,
                       var cmdLine: String? = null,
                       val args: Array<String> = emptyArray(),
                       val env: Map<String, String> = emptyMap(),
                       val directory: File? = null,
                       val inheritIO: Boolean = false,
                       val redirectErrorStream: Boolean = false,
                       val failOnExitCode: Boolean = false,
                       val failOnTimeout: Boolean = false,
                       val timeout: TimeSpec = TimeSpec.ZERO)
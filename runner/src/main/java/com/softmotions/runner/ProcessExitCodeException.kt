package com.softmotions.runner

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
class ProcessExitCodeException(cmd: String, val exitCode: Int) :
        RuntimeException("Process '$cmd' Exit code: $exitCode")
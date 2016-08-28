package com.softmotions.runner

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
class ProcessWaitTimeoutException(cmd: String) :
        RuntimeException("Process wait timeout: '$cmd'") {
}
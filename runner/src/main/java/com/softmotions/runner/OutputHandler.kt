package com.softmotions.runner

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
interface OutputHandler {
    operator fun invoke(line: String) = asFn()(line)
    fun asFn(): ((line: String) -> Unit)
}
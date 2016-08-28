package com.softmotions.runner

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
class OutputHandlerFnAdapter(val fn: (line: String) -> Unit) : OutputHandler {
    override fun asFn(): (String) -> Unit {
        return fn
    }
}
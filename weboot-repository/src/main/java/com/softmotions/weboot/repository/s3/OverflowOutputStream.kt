package com.softmotions.weboot.repository.s3

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.IllegalStateException

class OverflowOutputStream(
        val threshold: Long,
        val overflowStreamSupplier: (() -> OutputStream)) : OutputStream() {

    private val memoryOutput = MemoryOutput()

    private var counter = 0L

    private var overflowOutput: OutputStream? = null

    private val out: OutputStream
        get() {
            if (overflowOutput == null && counter > threshold) {
                overflowOutput = overflowStreamSupplier().apply {
                    write(memoryOutput.buffer, 0, memoryOutput.length)
                    memoryOutput.reset()
                    flush()
                }
            }
            return overflowOutput ?: memoryOutput
        }

    val length: Long
        get() = counter

    val inMemory: Boolean = overflowOutput == null

    fun copyMemoryBytes(os: OutputStream) {
        if (!inMemory) throw IllegalStateException("Overflow stream created")
        os.write(memoryOutput.buffer, 0, memoryOutput.length)
    }

    fun memoryToByteArrayInputStream(): ByteArrayInputStream {
        return ByteArrayInputStream(memoryOutput.buffer, 0, memoryOutput.length)
    }


    override fun write(b: Int) {
        ++counter
        out.write(b)
    }

    override fun write(b: ByteArray) {
        counter += b.size
        out.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        counter += len
        out.write(b, off, len)
    }

    override fun flush() = out.flush()

    override fun close() {
        out.close()
        counter = 0
        memoryOutput.reset()
        overflowOutput = null
    }

    private class MemoryOutput : ByteArrayOutputStream() {
        internal val buffer: ByteArray get() = buf
        internal val length: Int get() = count
        override fun reset() {
            super.reset()
            buf = ByteArray(0)
        }
    }
}
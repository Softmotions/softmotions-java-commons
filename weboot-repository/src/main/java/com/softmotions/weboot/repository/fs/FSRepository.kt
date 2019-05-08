package com.softmotions.weboot.repository.fs

import com.softmotions.kotlin.loggerFor
import com.softmotions.kotlin.toPath
import com.softmotions.weboot.repository.WBRepository
import com.softmotions.xconfig.XConfig
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong

class FSRepository
constructor(cfg: XConfig) : WBRepository {

    companion object {
        private val log = loggerFor()
    }

    private val root: Path = cfg.textPattern("root", ".")!!.toPath()

    private val seq = AtomicLong(0)

    private val sf = root.resolve("next.txt").toFile()

    init {
        if (cfg.boolPattern("cleanup", false)) {
            log.warn("Deleting fs repository: ${root}")
            root.toFile().deleteRecursively()
        }
        Files.createDirectories(root)
        if (!sf.exists()) {
            sf.writeText(seq.get().toString())
        } else if (sf.length() < 64) { // Avoid memory DOS attack
            seq.set(sf.readText().toLong())
        }
    }

    @Synchronized
    private fun nextPath(): String {
        val next = seq.incrementAndGet().toString()
        sf.writeText(next)
        val ret = StringBuilder(16)
        for (i in 0 until next.length) {
            if (i > 0 && i % 3 == 0) {
                ret.append('/')
            }
            ret.append(next[i])
        }
        return ret.toString()
    }

    override fun acceptUri(uri: URI): Boolean {
        return uri.scheme == "fs"
    }

    override fun fetchFileName(uri: URI): String {
        return Paths.get(uri.path).toString()
    }

    override fun persist(input: InputStream, fname: String): URI {
        val next = "${nextPath()}/$fname"
        val file = root.resolve(next).toFile()
        file.parentFile?.mkdirs()
        FileOutputStream(file).use {
            input.transferTo(it)
        }
        return URI("fs", next, null)
    }

    @Synchronized
    override fun remove(uri: URI): Boolean {
        val file = root.resolve(uri.schemeSpecificPart).toFile()
        return if (file.exists()) {
            file.delete()
            true
        } else {
            false
        }
    }

    override fun transferTo(uri: URI, output: OutputStream, closeOutput: Boolean) {
        val file = root.resolve(uri.schemeSpecificPart).toFile()
        try {
            file.inputStream().use {
                it.transferTo(output)
            }
        } finally {
            if (closeOutput) {
                output.close()
            }
        }
    }
}
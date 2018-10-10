package com.softmotions.weboot.testing.btc

import com.softmotions.kotlin.TimeSpec
import com.softmotions.kotlin.loggerFor
import com.softmotions.runner.ProcessRunners
import com.softmotions.runner.UnixSignal
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class BTCTestRunner(dataDir: File? = null,
                    private val cleanupDataDir: Boolean = false,
                    private val extraArgs: List<String> = emptyList(),
                    private val mode: String? = "regtest") {

    companion object {
        private val log = loggerFor()
    }

    private val dataDir = dataDir ?: kotlin.run {
        Paths.get(System.getProperty("user.home"), ".bitcoin", mode).toFile()
    }

    private val state = AtomicBoolean(false)

    private val runner = ProcessRunners.serial(verbose = true)

    private fun outputLine(line: String) {
        log.info(line)
    }

    fun start() {
        if (state.compareAndExchange(false, true)) {
            throw IllegalStateException("Already in started state")
        }
        log.info("Using dataDir: ${dataDir}")
        try {
            val latch = CountDownLatch(1)
            if (cleanupDataDir) {
                if (dataDir.exists()) {
                    FileUtils.deleteDirectory(dataDir)
                }
            }
            val m = if (mode != null) "-${mode}" else ""
            runner.cmd("bitcoind ${m} ${extraArgs.joinToString(" ")}",
                       failOnExitCode = true,
                       failOnTimeout = true) { line ->
                outputLine(line)
                if (line.contains("opencon thread start")) {
                    latch.countDown()
                }
            }
            latch.await(1, TimeUnit.MINUTES)
            if (latch.count > 0L) {
                runner.halt(TimeSpec.ONE_MIN, UnixSignal.SIGINT)
                throw RuntimeException("Failed to start bitcond")
            }
        } catch (e: Throwable) {
            state.set(false)
            throw e
        }
    }

    fun shutdown() {
        if (!state.compareAndExchange(true, false)) {
            throw IllegalStateException("Already in shutdown state")
        }
        if (!runner.halt(TimeSpec.ONE_MIN, UnixSignal.SIGINT)) {
            val msg = "Timeout during bitcoind shutdown"
            log.error(msg)
            throw RuntimeException(msg)
        }
    }
}
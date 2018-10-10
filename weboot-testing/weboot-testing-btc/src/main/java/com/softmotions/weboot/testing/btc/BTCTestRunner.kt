package com.softmotions.weboot.testing.btc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.softmotions.kotlin.TimeSpec
import com.softmotions.kotlin.loggerFor
import com.softmotions.runner.ProcessRunners
import com.softmotions.runner.UnixSignal
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
class BTCTestRunner(datadir: File? = null,
                    private val cleanupDataDir: Boolean = false,
                    private val extraArgs: List<String> = emptyList(),
                    private val mode: String? = "regtest") {

    companion object {
        private val log = loggerFor()
    }

    private val datadir = datadir ?: kotlin.run {
        Paths.get(System.getProperty("user.home"), ".bitcoin", mode).toFile()
    }

    private val state = AtomicBoolean(false)

    private val runner = ProcessRunners.serial(verbose = true)

    private val cliRunner = ProcessRunners.serial(verbose = true)

    private val mapper = ObjectMapper()

    private fun outputLine(line: String) {
        log.info(line)
    }

    fun start() {
        if (state.compareAndExchange(false, true)) {
            throw IllegalStateException("Already in started state")
        }
        log.info("Using datadir: ${datadir}")
        try {
            if (cleanupDataDir) {
                if (datadir.exists()) {
                    FileUtils.deleteDirectory(datadir)
                }
            }
            if (!datadir.isDirectory) {
                log.info("Creating directory: ${datadir}")
                datadir.mkdirs()
                if (!datadir.isDirectory) {
                    throw IOException("Cannot create directory: ${datadir}")
                }
            }
            val latch = CountDownLatch(1)
            val m = if (mode != null) "-${mode}" else ""
            runner.cmd("bitcoind ${m} -datadir=${datadir} ${extraArgs.joinToString(" ")}") { line ->
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
        if (!runner.halt(TimeSpec.HALF_MIN, UnixSignal.SIGINT)) {
            val msg = "Timeout during bitcoind shutdown"
            log.error(msg)
            runner.halt(TimeSpec.HALF_MIN, UnixSignal.SIGKILL)
            throw RuntimeException(msg)
        }
    }

    fun generate(nblocks: Int, address: String? = null): JsonNode {
        val m = if (mode != null) "-${mode}" else ""
        return with(cliRunner) {
            val exec = if (address == null) {
                "bitcoin-cli ${m} -datadir=${datadir} generate ${nblocks}"
            } else {
                "bitcoin-cli ${m} -datadir=${datadir} generatetoaddress ${nblocks} ${address}"
            }
            val obuf = StringBuilder()
            var ret: JsonNode? = null
            cmd(exec, failOnTimeout = true, failOnExitCode = true) { line ->
                obuf.append(line)
                obuf.append('\n')
            }.waitFor(TimeSpec.ONE_MIN) {
                ret = mapper.readTree(obuf.toString())
            }
            ret!!
        }
    }

    fun send(address: String, amount: Double) {
        val m = if (mode != null) "-${mode}" else ""
        with(cliRunner) {
            cmd("bitcoin-cli ${m} -datadir=${datadir} sendtoaddress ${address} ${amount}") { line ->
                outputLine(line)
            }.waitFor(TimeSpec.ONE_MIN)
        }
    }
}
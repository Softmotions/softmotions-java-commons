package com.softmotions.weboot.testing.pg

import com.softmotions.kotlin.TimeSpec
import com.softmotions.kotlin.loggerFor
import com.softmotions.kotlin.toSeconds
import com.softmotions.runner.ProcessRun
import com.softmotions.runner.ProcessRunner
import com.softmotions.runner.ProcessRunners
import com.softmotions.runner.UnixSignal
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class PostgresTestRunner(private val dbName: String,
                         private val dbPort: Int = 9231,
                         private val dbBin: String = "/usr/lib/postgresql/10/bin",
                         private val dbDirPrefix: String = "/dev/shm") : DatabaseTestRunner {

    companion object {
        private val log = loggerFor()
    }

    private val dbRunner: ProcessRunner = ProcessRunners.serial(verbose = true)

    private var dbDir: String? = null

    private fun outputLine(line: String) {
        log.info(line)
    }

    private fun checkExitCode(pr: ProcessRun) {
        val ecode = pr.process.exitValue()
        if (ecode != 0) {
            throw RuntimeException("Process failed with exit code: $ecode command: ${pr.command}")
        }
    }

    override fun setupDb(props: Map<String, Any>) {
        shutdownDb()

        System.setProperty("JDBC.env", "pgtest")
        System.setProperty("JDBC.url", "jdbc:postgresql://localhost:${dbPort}/postgres")
        System.setProperty("JDBC.driver", "org.postgresql.Driver")

        val started = AtomicBoolean(false)
        val locale = "en_US.UTF-8"

        dbDir = "${dbDirPrefix}/${dbName}${System.currentTimeMillis()}"
        log.info("Setup database, dir: $dbDir")
        with(dbRunner) {
            cmd("mkdir -p $dbDir")
            cmd("$dbBin/initdb" +
                        " --lc-messages=C" +
                        " --lc-collate=$locale --lc-ctype=$locale" +
                        " --lc-monetary=$locale --lc-numeric=$locale --lc-time=$locale" +
                        " -D $dbDir",
                env = mapOf("LC_ALL" to "C")) {
                outputLine(it)
            }.waitFor {
                checkExitCode(it)
            }
            cmd("$dbBin/postgres -D $dbDir -p $dbPort -o \"-c fsync=off -c synchronous_commit=off -c full_page_writes=off\"",
                failOnTimeout = true) {
                outputLine(it)
                if (it.trim().contains("database system is ready to accept connections")) {
                    synchronized(started) {
                        started.set(true)
                        (started as Object).notifyAll()
                    }
                }
            }
        }
        synchronized(started) {
            if (!started.get()) {
                (started as Object).wait(30.toSeconds().toMillis())
            }
        }
        if (!started.get()) {
            throw RuntimeException("Timeout of waiting for postgres server")
        }
    }

    override fun shutdownDb() {
        dbRunner.reset(TimeSpec.HALF_MIN, UnixSignal.SIGINT)
        dbDir?.let {
            log.info("Remove database dir: $dbDir")
            with(dbRunner) {
                cmd("rm -rf $dbDir")
            }.haltRunner(30.toSeconds())
            dbDir = null
        }
    }
}
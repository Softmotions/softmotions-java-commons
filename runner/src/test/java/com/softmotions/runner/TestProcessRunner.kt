package com.softmotions.runner

import com.softmotions.commons.io.Loader
import com.softmotions.kotlin.loggerFor
import com.softmotions.kotlin.toMilliseconds
import com.softmotions.kotlin.toMinutes
import com.softmotions.kotlin.toSeconds
import org.testng.Assert.*
import org.testng.annotations.Test
import java.nio.file.Paths
import java.util.*

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@Test
class TestProcessRunner {

    private val log = loggerFor<TestProcessRunner>()

    @Test
    fun testBasic() {
        val lines = HashSet<String>()
        val pspec = ProcessSpec("/bin/sh", args = arrayOf("-c", "echo 'c7a03426\\n78622762'"))
        val runner = ProcessRunners.serial(verbose = true)
        var ecode: Int = -1
        val stdout = OutputHandlerFnAdapter({
            lines += it
        })
        val pr = runner.spec(pspec, stdout)
        assertEquals(runner.tasksNum, 1)
        pr.waitFor {
            ecode = it.exitCode
        }
        assertTrue(runner.halt(1.toMinutes()))
        assertEquals(ecode, 0)
        assertTrue("c7a03426" in lines)
        assertTrue("78622762" in lines)
    }

    @Test
    fun testErrorOutput() {
        val lines = HashSet<String>()
        val pspec = ProcessSpec("/bin/sh", args = arrayOf("-c", "echo 'd1ef29df\\nb6b870d4' 1>&2"))
        val runner = ProcessRunners.serial(verbose = true)
        var ecode: Int = -1;
        val stdout = OutputHandlerFnAdapter({
            lines += it
        })
        runner.spec(pspec, null, stdout).waitFor {
            ecode = it.exitCode
        }
        assertEquals(ecode, 0)
        assertTrue("d1ef29df" in lines)
        assertTrue("b6b870d4" in lines)
    }

    @Test
    fun testRedirectErrorOutput() {
        val lines = HashSet<String>()
        val pspec = ProcessSpec("/bin/sh",
                args = arrayOf("-c", "echo '3d12d6c6\\nbb5053dd' 1>&2"),
                redirectErrorStream = true)
        val runner = ProcessRunners.serial(verbose = true)
        var ecode: Int = -1;
        val stdout = OutputHandlerFnAdapter({
            lines += it
        })
        runner.spec(pspec, stdout).waitFor {
            ecode = it.exitCode
        }
        assertEquals(ecode, 0)
        assertTrue("3d12d6c6" in lines)
        assertTrue("bb5053dd" in lines)
    }

    @Test
    fun testCmdLine() {
        val lines = HashSet<String>()
        ProcessRunners.serial(verbose = true)
                .cmd("/bin/sh -c \"echo '08fb997f\\n7818f073' 1>&2\"") {
                    lines += it
                }.haltRunner()
        assertTrue("08fb997f" in lines)
        assertTrue("7818f073" in lines)
    }


    @Test
    fun testCmdLine2() {
        val lines = HashSet<String>()
        val pr = ProcessRunners.serial(verbose = true)
        var wasLines: Boolean = false;
        pr.cmd("/bin/sh -c \"echo '08fb997f\\n7818f073' 1>&2\"") {
            lines += it
        }.waitFor { pr ->
            wasLines = ("08fb997f" in lines) && ("7818f073" in lines)
            Unit
        }
        assertTrue(wasLines)
        pr.halt()
    }

    fun testExceptionInLineHandler() {
        val lines = HashSet<String>()
        val pr = ProcessRunners.serial(verbose = true)
        var error: Exception? = null
        try {
            pr.cmd("/bin/sh -c \"echo '08fb997f\\n7818f073' 1>&2\"") {
                lines += it
                throw Exception("ccba900e")
            }.waitFor { pr ->
                Unit
            }
        } catch(e: Exception) {
            error = e;
        }
        assertTrue(error!!.message!!.contains("ccba900e"))
        assertTrue("08fb997f" in lines)
        pr.halt()
    }

    @Test
    fun testEnv() {
        val lines = HashSet<String>()
        ProcessRunners.serial(verbose = true)
                .cmd("/bin/sh -c \"echo ENV11=\${ENV11}\"",
                        env = mapOf("ENV11" to "bdd5c88e")) {
                    lines += it
                }.haltRunner()
        assertTrue("ENV11=bdd5c88e" in lines)
    }

    @Test
    fun testStdin() {
        val lines = HashSet<String>()
        ProcessRunners.serial(verbose = true)
                .cmd("/bin/sh -c 'while read a; do echo \$a; done'") {
                    lines += it
                }
                .writeln("278e2f76")
                .writeln("2ff99be3")
                .writeEnd()
                .haltRunner()

        assertTrue("278e2f76" in lines)
        assertTrue("2ff99be3" in lines)
    }

    @Test
    fun testTimeout() {
        val lines = HashSet<String>()
        val pspec = ProcessSpec("/bin/sh",
                args = arrayOf("-c", "cat"),
                redirectErrorStream = true)
        val runner = ProcessRunners.serial(verbose = true)
        var stdout = OutputHandlerFnAdapter({
            lines += it
        })
        val pr = runner.spec(pspec, stdout)
        assertEquals(runner.tasksNum, 1)
        assertFalse(runner.halt(2.toSeconds()))
        var err: Exception? = null
        try {
            pr.exitCode
        } catch(e: IllegalThreadStateException) {
            err = e;
        }
        assertNotNull(err)
        assertEquals(err!!.message, "process hasn't exited")
    }


    @Test
    fun testTabularData() {

        val url = Loader.getResourceAsUrl("com/softmotions/runner/tdata1.txt", javaClass)
        assertNotNull(url)
        val path = Paths.get(url.toURI()).toAbsolutePath().toString()
        val trc = TabularOutputCollector(
                expectHeader = true,
                minSplitSpaces = 2,
                lineTransform = {
                    it.toLowerCase()
                })

        val runner = ProcessRunners.serial(true, "testTabularData")
        runner.cmd("cat $path", outputFn = trc.asFn()).waitFor()

        assertEquals(trc.header.size, 7)
        assertEquals(trc.header[0], "container id")
        assertEquals(trc.header[1], "image")
        assertEquals(trc.header[2], "command")
        assertEquals(trc.header[3], "created")
        assertEquals(trc.header[4], "status")
        assertEquals(trc.header[5], "ports")
        assertEquals(trc.header[6], "names")

        assertEquals(trc.rows.size, 3)

        var l1 = trc.rows[0]
        assertEquals(l1.size, 7)
        assertEquals(l1[0], "f94c2a915b88")
        assertEquals(l1[1], "registry:2")
        assertEquals(l1[2], "\"/bin/registry serve \"")
        assertEquals(l1[3], "3 weeks ago")
        assertEquals(l1[4], "up 2 weeks")
        assertEquals(l1[5], "127.0.0.1:5000->5000/tcp")
        assertEquals(l1[6], "registry")

        l1 = trc.rows[1]
        assertEquals(l1.size, 7)
        assertEquals(l1[0], "f94c2a915b81")
        assertEquals(l1[1], "registry:2")
        assertEquals(l1[2], "\"/bin/registry serve \"")
        assertEquals(l1[3], "3 weeks ago")
        assertEquals(l1[4], "up 3 weeks")
        assertEquals(l1[5], "127.0.0.1:5001->5001/tcp")
        assertEquals(l1[6], "registry1")

        val nrows = trc.asNamedRows()
        var nr = nrows[0]
        assertEquals(nr["image"], "registry:2")
        assertEquals(nr["names"], "registry")
        assertEquals(nr["created"], "3 weeks ago")
        assertEquals(nr["container id"], "f94c2a915b88")
        assertEquals(nr["ports"], "127.0.0.1:5000->5000/tcp")
        assertEquals(nr["command"], "\"/bin/registry serve \"")
        assertEquals(nr["status"], "up 2 weeks")

        nr = nrows[2]
        assertEquals(nr["image"], "registry:2")
        assertEquals(nr["names"], "registry1")
        assertEquals(nr["created"], "3 weeks ago")
        assertEquals(nr["container id"], "f94c2a915b81")
        assertEquals(nr["ports"], "127.0.0.1:5001->5001/tcp")
        assertEquals(nr["command"], "")
        assertEquals(nr["status"], "")

        runner.halt();
    }

    @Test
    fun testExitCodeFail() {
        var msg: String? = null
        val runner = ProcessRunners.serial(true, "testExitCodeFail")
        runner.cmd("/bin/sh -c 'exit 11'", failOnExitCode = false).waitFor()
        try {
            runner.cmd("/bin/sh -c 'exit 22'", failOnExitCode = true).waitFor()
        } catch(e: ProcessExitCodeException) {
            msg = e.message
        }
        assertEquals(msg, "Process '/bin/sh -c exit 22' Exit code: 22")
        runner.cmd("/bin/sh -c 'exit 0'", failOnExitCode = true).waitFor()
        runner.halt()
    }


    @Test
    fun testFailOnTimeout() {
        var msg: String? = null
        ProcessRunners.serial(true, "testFailOnTimeout").use {
            try {
                it.cmd("/bin/sh -c 'sleep 1'", failOnTimeout = true)
                        .waitFor(500.toMilliseconds())
            } catch(e: ProcessWaitTimeoutException) {
                msg = e.message
            }
        }
        assertEquals(msg, "Process wait timeout: '/bin/sh -c sleep 1'")
    }
}
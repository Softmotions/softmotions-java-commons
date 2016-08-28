package com.softmotions.runner

import com.softmotions.commons.string.CmdArgumentsTokenizer
import com.softmotions.kotlin.TimeSpec
import com.softmotions.kotlin.loggerFor
import com.softmotions.kotlin.toMinutes
import java.io.File
import java.io.InputStream
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.properties.Delegates

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
object ProcessRunners {

    private val log = loggerFor<ProcessRunners>()

    private val outputTasksPool = Executors.newCachedThreadPool()

    fun serial(verbose: Boolean = false, group: String? = null): ProcessRunner {
        return ProcessRunnerImpl(verbose, "serial", group)
    }

    fun parallel(verbose: Boolean = false, group: String? = null): ProcessRunner {
        return ProcessRunnerImpl(verbose, "parallel", group)
    }


    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private class ProcessRunnerImpl(val verbose: Boolean = false,
                                    val type: String,
                                    val group: String?) : ProcessRunner {

        private val tasks = mutableListOf<ProcessTask>()
        private val startMon = AtomicBoolean(false)
        private val stopMon = AtomicBoolean(false)
        private var executor = newExecutor()


        private fun newExecutor(): ExecutorService {
            return when (type) {
                "parallel" -> newParallelThreadExecutor()
                else -> newSingleThreadExecutor()
            }
        }

        private fun newParallelThreadExecutor(): ExecutorService {
            Executors.newCachedThreadPool()
            return ThreadPoolExecutor(0, Math.max(Runtime.getRuntime().availableProcessors(), 2),
                    5L, TimeUnit.SECONDS,
                    LinkedBlockingQueue<Runnable>(1024 * 1024));
        }

        private fun newSingleThreadExecutor(): ExecutorService {
            return ThreadPoolExecutor(0, 1,
                    1, TimeUnit.MINUTES,
                    LinkedBlockingQueue<Runnable>(1024 * 1024))
        }

        override val tasksNum: Int
            get() {
                return synchronized(tasks) {
                    tasks.size
                }
            }

        override val userData: Map<String, Any> by lazy {
            ConcurrentHashMap<String, Any>()
        }

        override fun cmd(cmdLine: String,
                         directory: File?,
                         env: Map<String, String>,
                         failOnExitCode: Boolean,
                         failOnTimeout: Boolean,
                         stderrFn: ((line: String) -> Unit)?,
                         outputFn: ((String) -> Unit)?): ProcessRun {


            return spec(
                    ProcessSpec(
                            cmdLine = cmdLine,
                            redirectErrorStream = (stderrFn == null),
                            failOnTimeout = failOnTimeout,
                            failOnExitCode = failOnExitCode,
                            directory = directory,
                            env = env),
                    outputFn?.let { OutputHandlerFnAdapter(outputFn) },
                    stderrFn?.let { OutputHandlerFnAdapter(stderrFn) })
        }

        override fun spec(spec: ProcessSpec,
                          stdout: OutputHandler?,
                          stderr: OutputHandler?): ProcessRun {
            val task = ProcessTask(spec, stdout, stderr)
            synchronized(tasks) {
                tasks.add(task)
            }
            task.selfFuture = CompletableFuture.runAsync(task, executor)
            return task
        }

        override fun close() {
            halt(30.toMinutes())
        }

        override fun halt(maxWait: TimeSpec,
                          signal: UnixSignal?,
                          block: ((ProcessRunner) -> Unit)?): Boolean {
            if (signal != null) {
                synchronized(tasks) {
                    tasks.forEach {
                        it.process.kill(signal)
                    }
                }
            }
            executor.shutdown()
            val ret = awaitTermination(maxWait)
            synchronized(tasks) {
                tasks.forEach {
                    it.process.destroyForcibly()
                }
                tasks.clear()
            }
            executor.shutdownNow()
            block?.invoke(this)
            return ret
        }

        fun awaitTermination(ts: TimeSpec): Boolean {
            return executor.awaitTermination(ts.time, ts.unit)
        }

        override fun reset(maxWait: TimeSpec,
                           signal: UnixSignal?,
                           block: ((ProcessRunner) -> Unit)?): Boolean {
            val ret = halt(maxWait, signal) {
                executor = newExecutor()
                block?.invoke(this)
            }
            return ret
        }

        internal inner class ProcessTask(val spec: ProcessSpec,
                                         val stdout: OutputHandler?,
                                         val stderr: OutputHandler?) : Runnable, ProcessRun {

            override val process: Process

            override val command: String

            override val exitCode: Int
                get() = process.exitValue()

            internal var selfFuture: CompletableFuture<Void> by Delegates.notNull()

            private val outputTasks = mutableListOf<CompletableFuture<*>>()

            private val clist: List<String>


            init {
                clist = if (spec.cmdLine != null) {
                    CmdArgumentsTokenizer.tokenize(spec.cmdLine)
                } else {
                    listOf<String>(spec.exec as String, *spec.args)
                }
                command = clist.joinToString(" ")
                val pb = ProcessBuilder(clist)
                pb.environment() += spec.env
                if (spec.inheritIO) pb.inheritIO()
                if (spec.redirectErrorStream) {
                    pb.redirectErrorStream(true)
                }
                pb.directory(spec.directory)
                if (verbose) {
                    log.info("Executing: {}", pb.command())
                }
                process = pb.start()
            }

            override fun write(data: CharSequence, flush: Boolean): ProcessRun {
                val os = process.outputStream ?: throw IllegalStateException("Process stdin is not piped")
                val str = if (data is String) data else data.toString()
                os.write(str.toByteArray())
                if (flush) {
                    os.flush()
                }
                return this;
            }

            override fun writeln(data: CharSequence, flush: Boolean): ProcessRun {
                return write(data = (if (data is String) data else data.toString()) + "\n",
                        flush = true)
            }

            override fun writeEnd(): ProcessRun {
                process.outputStream?.close();
                return this
            }

            override fun plusAssign(line: CharSequence) {
                write(line)
            }

            override fun haltRunner(wait: TimeSpec,
                                    signal: UnixSignal?,
                                    block: ((ProcessRun) -> Any)?): Boolean {
                return this@ProcessRunnerImpl.halt(wait, signal) {
                    block?.invoke(this)
                }
            }

            override fun waitFor(wait: TimeSpec?,
                                 block: ((ProcessRun) -> Unit)?): Boolean {

                val pwait: TimeSpec? = synchronized(startMon) {
                    if (!startMon.get()) { // wait to to process start
                        val mon = startMon as java.lang.Object
                        if (wait != null) {
                            val ts = System.currentTimeMillis();
                            mon.wait(wait.toMillis())
                            val te = System.currentTimeMillis();
                            return@synchronized if (wait.toMillis() - (te - ts) > 0) {
                                TimeSpec(
                                        wait.toMillis() - (te - ts),
                                        TimeUnit.MILLISECONDS)
                            } else {
                                null
                            }
                        } else {
                            mon.wait()
                        }
                    }
                    return@synchronized wait
                }

                if (pwait == null && wait != null) { // wait time elapsed
                    if (spec.failOnTimeout) {
                        throw ProcessWaitTimeoutException("${clist.joinToString(" ")}")
                    }
                    return false
                }

                var ret: Boolean = startMon.get()
                if (ret) { // process started wait for termination
                    synchronized(stopMon) {
                        if (!stopMon.get()) {
                            val mon = stopMon as java.lang.Object
                            if (pwait != null) {
                                mon.wait(pwait.toMillis())
                            } else {
                                mon.wait()
                            }
                        }
                        ret = stopMon.get()
                    }
                }

                try {
                    selfFuture.join()
                } catch(ce: CompletionException) {
                    throw ce.cause ?: ce
                }

                if (spec.failOnTimeout && !ret) {
                    throw ProcessWaitTimeoutException("${clist.joinToString(" ")}")
                }

                block?.invoke(this)
                return ret
            }

            override fun run() {
                val oldThreadName = Thread.currentThread().name
                try {
                    if (group != null) {
                        Thread.currentThread().name = "${oldThreadName}-$group"
                    }
                    if (stdout != null && process.inputStream != null) {
                        val ttype = if (spec.redirectErrorStream) "STDALL" else "STDOUT"
                        val tname = "${Thread.currentThread().name}-$ttype:${clist.firstOrNull()}"
                        outputTasks += CompletableFuture.runAsync(
                                OutputLineHandler(this, stdout, process.inputStream, tname),
                                outputTasksPool)
                    }
                    if (stderr != null && !spec.redirectErrorStream && process.errorStream != null) {
                        outputTasks += CompletableFuture.runAsync(
                                OutputLineHandler(this, stderr, process.errorStream,
                                        "${Thread.currentThread().name}-STDERR:${clist.firstOrNull()}"),
                                outputTasksPool);
                    }
                    synchronized(startMon) {
                        startMon.set(true)
                        (startMon as java.lang.Object).notifyAll()
                    }
                    if (spec.timeout > 0) {
                        if (!process.waitFor(spec.timeout.time, spec.timeout.unit)) {
                            log.error("Process timeout: {}", spec)
                            process.destroyForcibly()
                        }
                    } else {
                        process.waitFor()
                    }
                    if (verbose) {
                        log.info("Process: {} finished. Exit code: {}", clist, exitCode)
                    }
                    if (!outputTasks.isEmpty()) {
                        try {
                            CompletableFuture.allOf(*outputTasks.toTypedArray()).join()
                        } catch(ce: CompletionException) {
                            throw ce.cause ?: ce
                        }
                    }
                    if (spec.failOnExitCode && exitCode != 0) {
                        throw ProcessExitCodeException(clist.joinToString(" "), exitCode)
                    }
                } catch(ignored: InterruptedException) {
                    log.warn("Process forcibly interrupted. Command: $clist")
                } finally {
                    Thread.currentThread().name = oldThreadName
                    synchronized(stopMon) {
                        stopMon.set(true)
                        (stopMon as java.lang.Object).notifyAll()
                    }
                    synchronized(tasks) {
                        tasks.remove(this)
                    }
                }
            }

            inner class OutputLineHandler(val process: ProcessTask,
                                          val handler: OutputHandler,
                                          val istream: InputStream,
                                          val tname: String) : Runnable {

                override fun run() {
                    val oldThreadName = Thread.currentThread().name
                    try {
                        Thread.currentThread().name = tname
                        val reader = istream.bufferedReader()
                        for (line in reader.lines()) {
                            handler(line)
                        }
                    } finally {
                        Thread.currentThread().name = oldThreadName
                    }
                }
            }
        }
    }
}

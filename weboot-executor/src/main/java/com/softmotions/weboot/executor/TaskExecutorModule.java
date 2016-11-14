package com.softmotions.weboot.executor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Dispose;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TaskExecutorModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutorModule.class);

    private final ServicesConfiguration cfg;


    public TaskExecutorModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    protected void configure() {

        boolean hasDefault = false;
        for (HierarchicalConfiguration<ImmutableNode> ecfg : cfg.xcfg().configurationsAt("executor")) {
            String name = ecfg.getString("name", null);
            if ("default".equals(name)) {
                name = null;
            }
            if (name == null) {
                if (hasDefault) { // Default executor already bounded
                    continue;
                }
                hasDefault = true;
            }
            int threads;
            if ("allcores".equalsIgnoreCase(ecfg.getString("threads-num"))) {
                threads = Runtime.getRuntime().availableProcessors();
            } else {
                threads = Math.max(1, ecfg.getInt("threads-num",
                                                  Runtime.getRuntime().availableProcessors()));
            }
            log.info("Using {} threads for tasks executor: {}", threads, (name != null ? name : "default"));
            BlockingQueue<Runnable> workQueue;
            Integer queueSize = ecfg.getInteger("queue-size", null);
            if (queueSize == null || queueSize < 0) {
                log.info("Using unbounded queue for tasks executor");
                workQueue = new LinkedBlockingQueue<>();
            } else if (queueSize == 0) {
                log.info("Using tasks executor without queue");
                workQueue = new SynchronousQueue<>();
            } else {
                log.info("Using bounded queue (size = {}) for tasks executor", queueSize);
                workQueue = new LinkedBlockingQueue<>(queueSize);
            }

            DelegateTaskExecutor executor = new DelegateTaskExecutor(
                    new ThreadPoolExecutor(0, threads,
                                           60L, TimeUnit.SECONDS,
                                           workQueue));
            if (name == null) {
                bind(TaskExecutor.class).toInstance(executor);

            }
            bind(TaskExecutor.class)
                    .annotatedWith(Names.named(name != null ? name : "default"))
                    .toInstance(executor);
        }

        if (!hasDefault) { // bind default executor

            int threads = Runtime.getRuntime().availableProcessors();
            log.info("Using {} threads for tasks executor: {}", threads, "default");
            DelegateTaskExecutor executor = new DelegateTaskExecutor(
                    new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(),
                                           60L, TimeUnit.SECONDS,
                                           new LinkedBlockingQueue<>()));
            bind(TaskExecutor.class).toInstance(executor);
            bind(TaskExecutor.class)
                    .annotatedWith(Names.named("default"))
                    .toInstance(executor);
        }
    }

    public static class DelegateTaskExecutor implements TaskExecutor {

        private final ExecutorService executor;

        DelegateTaskExecutor(ExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isShutdown() {
            return executor.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return executor.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return executor.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return executor.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return executor.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return executor.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return executor.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return executor.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return executor.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                               long timeout,
                               TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return executor.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            executor.execute(command);
        }

        @Dispose
        public void shutdownContainer() {
            try {
                executor.shutdown();
            } catch (Exception ignored) {
            }
        }
    }
}

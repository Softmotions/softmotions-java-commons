package com.softmotions.commons.io.watcher;

import com.softmotions.commons.UserDataStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.Watchable;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * File system changes watcher.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */

@ThreadSafe
public class FSWatcher implements Closeable, Runnable, UserDataStore {

    private static final Logger log = LoggerFactory.getLogger(FSWatcher.class);

    private final Object lock = new Object();

    private final WatchService ws;

    private final FileSystem fileSystem;

    private final Set<WatchKey> keys;

    private final Map<WatchKey, WatchSlot> slots;

    private final FSWatcherEventHandler handler;

    private final long pollTimeoutMills;

    private Thread watchThread;

    private String name;

    private Object userData;


    public FSWatcher(String name, FSWatcherEventHandler handler, long pollTimeoutMills) throws IOException {
        this(name, FileSystems.getDefault(), handler, pollTimeoutMills);
    }

    public FSWatcher(String name, FileSystem fileSystem, FSWatcherEventHandler handler, long pollTimeoutMills) throws IOException {
        this.name = name;
        this.fileSystem = fileSystem;
        this.keys = new HashSet<>();
        this.slots = new HashMap<>();
        this.ws = fileSystem.newWatchService();
        this.handler = handler;
        this.pollTimeoutMills = (pollTimeoutMills <= 0) ? Long.MAX_VALUE : pollTimeoutMills;
        this.watchThread = null;
        this.initWatchingThread();
        if (this.handler != null) {
            this.handler.init(this);
        }
    }

    @Override
    public <T> T getUserData() {
        synchronized (lock) {
            return (T) userData;
        }
    }

    @Override
    public <T> void setUserData(T data) {
        synchronized (lock) {
            userData = data;
        }
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public void register(File file, boolean recursive) throws IOException {
        register(file.toPath(), recursive);
    }

    public void register(String path, boolean recursive) throws IOException {
        register(Paths.get(path), recursive);
    }

    public void register(Path path, final boolean recursive) throws IOException {
        if (recursive) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    register(dir, new WatchSlot(true));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    fireRegister(new FSWatcherRegisterEvent(FSWatcher.this, file));
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            register(path, new WatchSlot(false));
        }
    }

    private void register(Path path, WatchSlot slot) throws IOException {
        ensureWatchingThread();
        synchronized (lock) {
            WatchKey key = path.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            if (!keys.contains(key)) {
                keys.add(key);
                slots.put(key, slot);
            }
        }
        fireRegister(new FSWatcherRegisterEvent(this, path));
    }

    public void unregister(File path) throws IOException {
        unregister(path.toPath());
    }

    public void unregister(String path) throws IOException {
        unregister(Paths.get(path));
    }


    public void unregister(Path path) throws IOException {
        WatchSlot slot = unregisterFlat(path);
        if (slot != null && slot.recursive) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    unregisterFlat(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private WatchSlot unregisterFlat(Path path) throws IOException {
        WatchSlot slot = null;
        synchronized (lock) {
            WatchKey found = null;
            for (WatchKey k : keys) {
                Watchable w = k.watchable();
                if (w instanceof Path) {
                    Path wp = (Path) w;
                    if (Files.isSameFile(path, wp)) {
                        found = k;
                        break;
                    }
                }
            }
            if (found != null) {
                found.cancel();
                keys.remove(found);
                slot = slots.remove(found);
            }
        }
        return slot;
    }

    public void reset() throws IOException {
        synchronized (lock) {
            for (final WatchKey k : keys) {
                k.cancel();
            }
            keys.clear();
            slots.clear();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            synchronized (lock) {
                destroyWatchingThread();
                for (WatchKey k : keys) {
                    k.cancel();
                }
                keys.clear();
                slots.clear();
            }
        } finally {
            ws.close();
        }
    }

    public void join() throws InterruptedException {
        join(Long.MAX_VALUE);
    }

    public void join(long maxWaitMills) throws InterruptedException {
        Thread wt;
        synchronized (lock) {
            wt = this.watchThread;
        }
        if (wt != null) {
            wt.join(maxWaitMills);
        }
    }

    private void ensureWatchingThread() {
        synchronized (lock) {
            if (watchThread == null ||
                !watchThread.isAlive() ||
                watchThread.isInterrupted()) {
                initWatchingThread();
            }
        }
    }

    private void initWatchingThread() {
        synchronized (lock) {
            destroyWatchingThread();
            watchThread = new Thread(this, toString());
            watchThread.start();
        }
    }

    private void destroyWatchingThread() {
        synchronized (lock) {
            if (watchThread != null && watchThread.isAlive()) {
                watchThread.interrupt();
            }
        }
    }

    protected void fireCreate(FSWatcherCreateEvent ev) {
        if (handler != null) {
            try {
                handler.handleCreateEvent(ev);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    protected void fireModify(FSWatcherModifyEvent ev) {
        if (handler != null) {
            try {
                handler.handleModifyEvent(ev);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    protected void fireDelete(FSWatcherDeleteEvent ev) {
        if (handler != null) {
            try {
                handler.handleDeleteEvent(ev);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }


    protected void fireRegister(FSWatcherRegisterEvent ev) {
        if (handler != null) {
            try {
                handler.handleRegisterEvent(ev);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }


    @Override
    public void run() {
        log.info("Starting watcher thread: " + Thread.currentThread().getName());
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = ws.poll(pollTimeoutMills, TimeUnit.MILLISECONDS);
                if (key == null) {
                    if (handler != null) {
                        try {
                            handler.handlePollTimeout(this);
                        } catch (Exception e) {
                            log.error("", e);
                        }
                    }
                    continue;
                }
            } catch (ClosedWatchServiceException | InterruptedException e) {
                break;
            }
            WatchSlot slot;
            synchronized (lock) {
                slot = slots.get(key);
            }
            if (slot == null) {
                continue;
            }
            for (WatchEvent ev : key.pollEvents()) {
                WatchEvent.Kind kind = ev.kind();
                if (kind == OVERFLOW) {
                    continue;
                }
                Path dir = (Path) key.watchable();
                Path child = (Path) ev.context();
                if (kind == ENTRY_MODIFY) {
                    fireModify(new FSWatcherModifyEvent(this, dir, child));
                } else if (kind == ENTRY_CREATE) {
                    if (slot.recursive) {
                        child = dir.resolve(child);
                        if (Files.isDirectory(child)) {
                            try {
                                register(child, true);
                            } catch (IOException e) {
                                log.error("", e);
                            }
                        }
                    }
                    fireCreate(new FSWatcherCreateEvent(this, dir, child));
                } else if (kind == ENTRY_DELETE) {
                    fireDelete(new FSWatcherDeleteEvent(this, dir, child));
                } else {
                    log.error("Unknown event type: " + kind);
                }
                if (!key.reset()) {
                    synchronized (lock) {
                        keys.remove(key);
                        slots.remove(key);
                    }
                }
            }
        }
        log.warn("Watcher thread: " + Thread.currentThread().getName() + " finished");
    }

    public String toString() {
        return getClass().getSimpleName() + '[' + name + ']';
    }

    private static final class WatchSlot {

        private final boolean recursive;

        private WatchSlot(boolean recursive) {
            this.recursive = recursive;
        }
    }
}

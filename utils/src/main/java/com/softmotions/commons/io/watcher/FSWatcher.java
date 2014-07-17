package com.softmotions.commons.io.watcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * File system changes watcher.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcher implements Closeable, Runnable {

    private static final Logger log = LoggerFactory.getLogger(FSWatcher.class);

    private final Object lock = new Object();

    private final WatchService ws;

    private final FileSystem fileSystem;

    private final Set<WatchKey> keys;

    private final Map<WatchKey, WatchSlot> slots;

    private final FSWatcherEventHandler handler;

    private Thread watchThread;


    public FSWatcher(FSWatcherEventHandler handler) throws IOException {
        this(FileSystems.getDefault(), handler);
    }

    public FSWatcher(FileSystem fileSystem, FSWatcherEventHandler handler) throws IOException {
        this.fileSystem = fileSystem;
        this.keys = new HashSet<>();
        this.slots = new HashMap<>();
        this.ws = fileSystem.newWatchService();
        this.handler = handler;
        this.watchThread = null;
        this.initWatchingThread();
        if (this.handler != null) {
            this.handler.init(this);
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

    public void register(Path path, boolean recursive) throws IOException {
        register(path, new WatchSlot(recursive));
        if (recursive) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    register(dir, true);
                    return FileVisitResult.CONTINUE;
                }
            });
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
            handler.handleCreateEvent(ev);
        }
    }


    protected void fireModify(FSWatcherModifyEvent ev) {
        if (handler != null) {
            handler.handleModifyEvent(ev);
        }
    }

    protected void fireDelete(FSWatcherDeleteEvent ev) {
        if (handler != null) {
            handler.handleDeleteEvent(ev);
        }
    }


    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = ws.take();
            } catch (ClosedWatchServiceException | InterruptedException e) {
                log.warn("Watcher thread: " + Thread.currentThread().getName() + " finished");
                return;
            }
            WatchSlot slot;
            synchronized (lock) {
                slot = slots.get(key);
            }
            if (slot == null) {
                log.error("No slots found for watch key=" + key);
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
    }


    private static final class WatchSlot {

        private final boolean recursive;

        private WatchSlot(boolean recursive) {
            this.recursive = recursive;
        }
    }
}

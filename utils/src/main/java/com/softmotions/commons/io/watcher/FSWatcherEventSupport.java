package com.softmotions.commons.io.watcher;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * Base class form {@link FSWatcher} EventBus events
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherEventSupport implements Serializable {

    final FSWatcher watcher;

    final Path directory;

    final Path child;

    final Path fullPath;


    public FSWatcher getWatcher() {
        return watcher;
    }

    public Path getDirectory() {
        return directory;
    }

    public Path getChild() {
        return child;
    }

    public Path getFullPath() {
        return fullPath;
    }

    public FSWatcherEventSupport(FSWatcher watcher, Path directory, Path child) {
        this.watcher = watcher;
        this.directory = directory;
        this.child = child;
        this.fullPath = directory.resolve(child);
    }

    public String toString() {
        return getClass().getSimpleName() + '[' + directory + ", " + child + ']';
    }
}

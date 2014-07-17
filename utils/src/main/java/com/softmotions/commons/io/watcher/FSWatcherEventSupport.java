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


    public FSWatcher getWatcher() {
        return watcher;
    }

    public Path getDirectory() {
        return directory;
    }

    public Path getChild() {
        return child;
    }

    public FSWatcherEventSupport(FSWatcher watcher, Path directory, Path child) {
        this.watcher = watcher;
        this.directory = directory;
        this.child = child;
    }

    public String toString() {
        return getClass().getSimpleName() + '[' + directory + ", " + child + ']';
    }
}

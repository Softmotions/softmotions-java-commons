package com.softmotions.commons.io.watcher;

import java.io.Serializable;
import java.nio.file.Path;

/**
 * Base class form {@link FSWatcher} EventBus events
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherEventSupport implements Serializable {

    final Path directory;

    final Path child;


    public Path getDirectory() {
        return directory;
    }

    public Path getChild() {
        return child;
    }

    public FSWatcherEventSupport(Path directory, Path child) {
        this.directory = directory;
        this.child = child;
    }

    public String toString() {
        return getClass().getSimpleName() + "[" + directory + ", " + child + ']';
    }
}

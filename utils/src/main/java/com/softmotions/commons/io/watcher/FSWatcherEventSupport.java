package com.softmotions.commons.io.watcher;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

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
        this.fullPath = directory.resolve(child).normalize().toAbsolutePath();
    }

    public String toString() {
        return getClass().getSimpleName() + '[' + directory + ", " + child + ']';
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FSWatcherEventSupport that = (FSWatcherEventSupport) o;
        return Objects.equals(fullPath, that.fullPath);
    }

    public int hashCode() {
        return Objects.hash(fullPath);
    }
}

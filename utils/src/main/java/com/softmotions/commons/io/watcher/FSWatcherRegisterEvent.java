package com.softmotions.commons.io.watcher;

import java.nio.file.Path;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherRegisterEvent {

    final FSWatcher watcher;

    final Path fullPath;

    public Path getFullPath() {
        return fullPath;
    }

    public FSWatcher getWatcher() {
        return watcher;
    }

    public FSWatcherRegisterEvent(FSWatcher watcher, Path fullPath) {
        this.watcher = watcher;
        this.fullPath = fullPath;
    }

    public String toString() {
        return getClass().getSimpleName() + '[' + fullPath + ']';
    }
}

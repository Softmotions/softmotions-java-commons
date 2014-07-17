package com.softmotions.commons.io.watcher;

import java.nio.file.Path;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherCreateEvent extends FSWatcherEventSupport {

    public FSWatcherCreateEvent(FSWatcher watcher, Path directory, Path child) {
        super(watcher, directory, child);
    }
}

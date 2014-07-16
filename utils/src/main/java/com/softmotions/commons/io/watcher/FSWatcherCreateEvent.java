package com.softmotions.commons.io.watcher;

import java.nio.file.Path;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherCreateEvent extends FSWatcherEventSupport {

    public FSWatcherCreateEvent(Path directory, Path child) {
        super(directory, child);
    }
}

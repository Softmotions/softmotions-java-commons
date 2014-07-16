package com.softmotions.commons.io.watcher;

import java.nio.file.Path;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherModifyEvent extends FSWatcherEventSupport {

    public FSWatcherModifyEvent(Path directory, Path child) {
        super(directory, child);
    }
}

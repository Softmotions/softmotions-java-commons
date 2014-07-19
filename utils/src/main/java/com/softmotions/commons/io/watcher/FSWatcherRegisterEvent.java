package com.softmotions.commons.io.watcher;

import java.nio.file.Path;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherRegisterEvent extends FSWatcherEventSupport {

    public FSWatcherRegisterEvent(FSWatcher watcher, Path fullPath) {
        super(watcher, fullPath.getParent(), fullPath.getFileName());
    }

    public String toString() {
        return getClass().getSimpleName() + '[' + fullPath + ']';
    }
}

package com.softmotions.commons.io.scanner;

import com.softmotions.commons.io.watcher.FSWatcher;
import com.softmotions.commons.io.watcher.FSWatcherEventHandler;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface DirectoryScanner extends Closeable {

    void scan(DirectoryScannerVisitor visitor) throws IOException;

    FSWatcher activateFileSystemWatcher(FSWatcherEventHandler handler) throws IOException;
}

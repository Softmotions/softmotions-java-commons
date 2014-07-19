package com.softmotions.commons.io.scanner;

import com.softmotions.commons.UserDataStore;
import com.softmotions.commons.io.watcher.FSWatcher;
import com.softmotions.commons.io.watcher.FSWatcherEventHandler;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Directory scanner produced by
 * {@link com.softmotions.commons.io.scanner.DirectoryScannerFactory}.
 * <p/>
 * Implementation may not be thread-safe.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface DirectoryScanner extends Closeable, UserDataStore {

    Path getBasedir();

    void scan(DirectoryScannerVisitor visitor) throws IOException;

    FSWatcher activateFileSystemWatcher(FSWatcherEventHandler handler) throws IOException;

    FSWatcher activateFileSystemWatcher(FSWatcherEventHandler handler,
                                        long pollTimeoutMills,
                                        Object userData) throws IOException;
}

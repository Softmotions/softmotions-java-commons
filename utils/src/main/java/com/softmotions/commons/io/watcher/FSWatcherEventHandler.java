package com.softmotions.commons.io.watcher;

import java.io.IOException;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface FSWatcherEventHandler {

    void init(FSWatcher watcher) throws IOException;

    void handleRegisterEvent(FSWatcherRegisterEvent ev);

    void handleCreateEvent(FSWatcherCreateEvent ev);

    void handleDeleteEvent(FSWatcherDeleteEvent ev);

    void handleModifyEvent(FSWatcherModifyEvent ev);
}

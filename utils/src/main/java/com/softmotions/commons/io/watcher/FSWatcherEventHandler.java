package com.softmotions.commons.io.watcher;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface FSWatcherEventHandler {

    void init(FSWatcher watcher);

    void handlePollTimeout(FSWatcher watcher) throws Exception;

    void handleRegisterEvent(FSWatcherRegisterEvent ev) throws Exception;

    void handleCreateEvent(FSWatcherCreateEvent ev) throws Exception;

    void handleDeleteEvent(FSWatcherDeleteEvent ev) throws Exception;

    void handleModifyEvent(FSWatcherModifyEvent ev) throws Exception;
}

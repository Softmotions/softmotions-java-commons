package com.softmotions.commons.io.watcher;

import com.softmotions.commons.ebus.EBus;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherEBusEventHandler implements FSWatcherEventHandler {

    private final EBus ebus;

    public FSWatcherEBusEventHandler(EBus ebus) {
        this.ebus = ebus;
    }

    @Override
    public void init(FSWatcher watcher) {

    }

    @Override
    public void handlePollTimeout(FSWatcher watcher) {

    }

    @Override
    public void handleRegisterEvent(FSWatcherRegisterEvent ev) {
        ebus.fire(ev);
    }

    @Override
    public void handleCreateEvent(FSWatcherCreateEvent ev) {
        ebus.fire(ev);
    }

    @Override
    public void handleDeleteEvent(FSWatcherDeleteEvent ev) {
        ebus.fire(ev);
    }

    @Override
    public void handleModifyEvent(FSWatcherModifyEvent ev) {
        ebus.fire(ev);
    }
}

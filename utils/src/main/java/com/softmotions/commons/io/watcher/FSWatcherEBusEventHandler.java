package com.softmotions.commons.io.watcher;

import com.softmotions.commons.ebus.EBus;

import java.io.IOException;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherEBusEventHandler implements FSWatcherEventHandler {

    private final EBus ebus;

    public FSWatcherEBusEventHandler(EBus ebus) {
        this.ebus = ebus;
    }

    public void init(FSWatcher watcher) throws IOException {

    }

    public void handleRegisterEvent(FSWatcherRegisterEvent ev) {
        ebus.fire(ev);
    }

    public void handleCreateEvent(FSWatcherCreateEvent ev) {
        ebus.fire(ev);
    }

    public void handleDeleteEvent(FSWatcherDeleteEvent ev) {
        ebus.fire(ev);
    }

    public void handleModifyEvent(FSWatcherModifyEvent ev) {
        ebus.fire(ev);
    }
}

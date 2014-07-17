package com.softmotions.commons.io.watcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherCollectEventHandler implements FSWatcherEventHandler {

    private final Set<Path> registered = new HashSet<>();

    private final Set<Path> created = new HashSet<>();

    private final Set<Path> deleted = new HashSet<>();

    private final Set<Path> modified = new HashSet<>();

    private final Path basedir;

    public FSWatcherCollectEventHandler() {
        this(null);
    }

    public FSWatcherCollectEventHandler(Path basedir) {
        this.basedir = basedir;
    }

    public Set<Path> getRegistered() {
        return registered;
    }

    public Set<Path> getCreated() {
        return created;
    }

    public Set<Path> getDeleted() {
        return deleted;
    }

    public Set<Path> getModified() {
        return modified;
    }

    public void clear() {
        created.clear();
        deleted.clear();
        modified.clear();
        registered.clear();
    }


    public void init(FSWatcher watcher) throws IOException {

    }

    public void handleRegisterEvent(FSWatcherRegisterEvent ev) {
        registered.add(toPath(ev.getFullPath()));
    }

    public void handleCreateEvent(FSWatcherCreateEvent ev) {
        created.add(toPath(ev.getFullPath()));
    }

    public void handleDeleteEvent(FSWatcherDeleteEvent ev) {
        deleted.add(toPath(ev.getFullPath()));
    }

    public void handleModifyEvent(FSWatcherModifyEvent ev) {
        modified.add(toPath(ev.getFullPath()));
    }

    private Path toPath(Path p) {
        if (basedir != null) {
            try {
                p = basedir.relativize(p);
            } catch (Exception e) {
                ;
            }
        }
        return p;
    }
}

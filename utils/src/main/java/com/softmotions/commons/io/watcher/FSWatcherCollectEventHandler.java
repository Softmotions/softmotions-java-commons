package com.softmotions.commons.io.watcher;

import net.jcip.annotations.NotThreadSafe;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */

@NotThreadSafe
public class FSWatcherCollectEventHandler implements FSWatcherEventHandler, Cloneable {

    public static final int MOVE_CREATED_INTO_MODIFIED = 1;

    private ArrayList<Path> registered;

    private ArrayList<Path> created;

    private ArrayList<Path> deleted;

    private ArrayList<Path> modified;

    private Path basedir;

    private int flags;

    public FSWatcherCollectEventHandler() {
        this(null);
    }

    public FSWatcherCollectEventHandler(Path basedir) {
        this.basedir = basedir;
    }

    public FSWatcherCollectEventHandler(int flags) {
        this.basedir = null;
        this.flags = flags;
    }

    public FSWatcherCollectEventHandler(Path basedir, int flags) {
        this.flags = flags;
        this.basedir = basedir;
    }

    public List<Path> getRegistered() {
        return (registered != null) ? registered : Collections.EMPTY_LIST;
    }

    public List<Path> getCreated() {
        return (created != null) ? created : Collections.EMPTY_LIST;
    }

    public List<Path> getDeleted() {
        return (deleted != null) ? deleted : Collections.EMPTY_LIST;
    }

    public List<Path> getModified() {
        return (modified != null) ? modified : Collections.EMPTY_LIST;
    }

    public int getFlags() {
        return flags;
    }

    public void clear() {
        if (created != null) {
            created.clear();
        }
        if (deleted != null) {
            deleted.clear();
        }
        if (modified != null) {
            modified.clear();
        }
        if (registered != null) {
            registered.clear();
        }
    }

    public void init(FSWatcher watcher) {

    }

    public void handlePollTimeout(FSWatcher watcher) {

    }

    public void handleRegisterEvent(FSWatcherRegisterEvent ev) {
        if (registered == null) {
            registered = new ArrayList<>();
        }
        registered.add(toPath(ev.getFullPath()));
    }

    public void handleCreateEvent(FSWatcherCreateEvent ev) {
        if ((flags & MOVE_CREATED_INTO_MODIFIED) != 0) {
            if (modified == null) {
                modified = new ArrayList<>();
            }
            modified.add(toPath(ev.getFullPath()));
        } else {
            if (created == null) {
                created = new ArrayList<>();
            }
            created.add(toPath(ev.getFullPath()));
        }
    }

    public void handleDeleteEvent(FSWatcherDeleteEvent ev) {
        if (deleted == null) {
            deleted = new ArrayList<>();
        }
        deleted.add(toPath(ev.getFullPath()));
    }

    public void handleModifyEvent(FSWatcherModifyEvent ev) {
        if (modified == null) {
            modified = new ArrayList<>();
        }
        modified.add(toPath(ev.getFullPath()));
    }


    public Object clone() {
        FSWatcherCollectEventHandler cloned = new FSWatcherCollectEventHandler();
        cloned.basedir = basedir;
        cloned.flags = flags;
        cloned.registered = (registered != null && !registered.isEmpty()) ? (ArrayList<Path>) registered.clone() : null;
        cloned.modified = (modified != null && !modified.isEmpty()) ? (ArrayList<Path>) modified.clone() : null;
        cloned.created = (created != null && !created.isEmpty()) ? (ArrayList<Path>) created.clone() : null;
        cloned.deleted = (deleted != null && !deleted.isEmpty()) ? (ArrayList<Path>) deleted.clone() : null;
        return cloned;
    }

    private Path toPath(Path p) {
        if (basedir != null) {
            try {
                p = basedir.relativize(p);
            } catch (Exception ignored) {
            }
        }
        return p;
    }
}

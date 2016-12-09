package com.softmotions.commons.io.watcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FSWatcherCollectEventHandler2 implements FSWatcherEventHandler, Cloneable {

    public static final int MOVE_CREATED_INTO_MODIFIED = 1;

    private ArrayList<FSWatcherEventSupport> registered;

    private ArrayList<FSWatcherEventSupport> created;

    private ArrayList<FSWatcherEventSupport> deleted;

    private ArrayList<FSWatcherEventSupport> modified;

    private int flags;

    public FSWatcherCollectEventHandler2() {
        this(0);
    }

    public FSWatcherCollectEventHandler2(int flags) {
        this.flags = flags;
    }

    public List<FSWatcherEventSupport> getRegistered() {
        return (registered != null) ? registered : Collections.EMPTY_LIST;
    }

    public List<FSWatcherEventSupport> getCreated() {
        return (created != null) ? created : Collections.EMPTY_LIST;
    }

    public List<FSWatcherEventSupport> getDeleted() {
        return (deleted != null) ? deleted : Collections.EMPTY_LIST;
    }

    public List<FSWatcherEventSupport> getModified() {
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

    @Override
    public void init(FSWatcher watcher) {

    }

    @Override
    public void handlePollTimeout(FSWatcher watcher) {

    }

    @Override
    public void handleRegisterEvent(FSWatcherRegisterEvent ev) {
        if (registered == null) {
            registered = new ArrayList<>();
        }
        registered.add(ev);
    }

    @Override
    public void handleCreateEvent(FSWatcherCreateEvent ev) {
        if ((flags & MOVE_CREATED_INTO_MODIFIED) != 0) {
            if (modified == null) {
                modified = new ArrayList<>();
            }
            modified.add(ev);
        } else {
            if (created == null) {
                created = new ArrayList<>();
            }
            created.add(ev);
        }
    }

    @Override
    public void handleDeleteEvent(FSWatcherDeleteEvent ev) {
        if (deleted == null) {
            deleted = new ArrayList<>();
        }
        deleted.add(ev);
    }

    @Override
    public void handleModifyEvent(FSWatcherModifyEvent ev) {
        if (modified == null) {
            modified = new ArrayList<>();
        }
        modified.add(ev);
    }

    @Override
    public Object clone() {
        FSWatcherCollectEventHandler2 cloned = new FSWatcherCollectEventHandler2();
        cloned.flags = this.flags;
        cloned.registered = (registered != null && !registered.isEmpty()) ? (ArrayList<FSWatcherEventSupport>) registered.clone() : null;
        cloned.modified = (modified != null && !modified.isEmpty()) ? (ArrayList<FSWatcherEventSupport>) modified.clone() : null;
        cloned.created = (created != null && !created.isEmpty()) ? (ArrayList<FSWatcherEventSupport>) created.clone() : null;
        cloned.deleted = (deleted != null && !deleted.isEmpty()) ? (ArrayList<FSWatcherEventSupport>) deleted.clone() : null;
        return cloned;
    }
}


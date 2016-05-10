package com.softmotions.weboot.cayenne;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DumbCayenneContext extends CayenneContext {
    /**
     * Creates a new CayenneContext with no channel and disabled graph events.
     */
    public DumbCayenneContext() {
    }

    private DumbCayenneContext(DataChannel channel) {
        super(channel);
    }

    private DumbCayenneContext(DataChannel channel, boolean changeEventsEnabled, boolean lifecyleEventsEnabled) {
        super(channel, changeEventsEnabled, lifecyleEventsEnabled);
    }

    @Override
    public void registerNewObject(Object object) {
    }


    @Override
    protected boolean attachToRuntimeIfNeeded() {
        return false;
    }

    @Override
    public void propertyChanged(Persistent object, String property, Object oldValue, Object newValue) {
    }

    @Override
    public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
    }

    @Override
    protected void injectInitialValue(Object obj) {
        ((Persistent) obj).setPersistenceState(PersistenceState.TRANSIENT);
    }
}

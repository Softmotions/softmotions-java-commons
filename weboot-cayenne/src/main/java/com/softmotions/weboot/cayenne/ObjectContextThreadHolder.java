package com.softmotions.weboot.cayenne;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;

import com.softmotions.commons.ThreadUtils;

/**
 * Placeholder for the current thread object context.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ObjectContextThreadHolder {

    static final ThreadLocal<ObjectContext> contextStore = ThreadUtils.createInheritableThreadLocal();

    private ObjectContextThreadHolder() {
    }

    public static void setObjectContext(ObjectContext ctx) {
        contextStore.set(ctx);
    }

   @Nullable
    public static ObjectContext getObjectContext() {
        return contextStore.get();
    }

    public static void removeObjectContext() {
        contextStore.remove();
    }

    @Nonnull
    public static ObjectContext getOrCreate(ServerRuntime crt) {
        ObjectContext octx = contextStore.get();
        if (octx == null) {
            octx = crt.newContext();
            contextStore.set(octx);
        }
        return octx;
    }
}

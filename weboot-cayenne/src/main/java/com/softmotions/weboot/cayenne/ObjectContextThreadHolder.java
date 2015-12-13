package com.softmotions.weboot.cayenne;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.tx.Transaction;

/**
 * Placeholder for the current thread object context.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ObjectContextThreadHolder {

    static final ThreadLocal<ObjectContext> contextStore = new InheritableThreadLocal<>();

    private ObjectContextThreadHolder() {
    }

    public static void setObjectContext(ObjectContext ctx) {
        contextStore.set(ctx);
    }

    public static ObjectContext getObjectContext() {
        return contextStore.get();
    }

    public static void removeObjectContext() {
        contextStore.remove();
    }
}

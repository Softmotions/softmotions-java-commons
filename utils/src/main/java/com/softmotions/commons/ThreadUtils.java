package com.softmotions.commons;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ThreadUtils {

    static final List<ThreadLocal> locals = new CopyOnWriteArrayList<>();

    public static <T> ThreadLocal<T> createThreadLocal() {
        ThreadLocal<T> tl = new ThreadLocal<>();
        locals.add(tl);
        return tl;
    }

    public static void cleanThreadLocals() {
        for (ThreadLocal tl : locals) {
            tl.remove();
        }
    }

    public void cleanRefs() {
        locals.clear();
    }

    private ThreadUtils() {
    }
}

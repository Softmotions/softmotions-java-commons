package com.softmotions.commons;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ThreadUtils {

    private ThreadUtils() {
    }

    public static void cleanThreadLocals() {
        cleanThreadLocals(false);
    }

    public static void cleanInheritableThreadLocals() {
        cleanThreadLocals(false);
    }

    public static void cleanThreadLocals(boolean inheritable) {
        try {
            Thread thread = Thread.currentThread();
            Field threadLocalsField = Thread.class.getDeclaredField(inheritable ? "threadLocals" : "inheritableThreadLocals");
            threadLocalsField.setAccessible(true);
            Object threadLocalTable = threadLocalsField.get(thread);

            Class threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            Field tableField = threadLocalMapClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Object table = tableField.get(threadLocalTable);

            Field referentField = Reference.class.getDeclaredField("referent");
            referentField.setAccessible(true);
            for (int i = 0; i < Array.getLength(table); i++) {
                Object entry = Array.get(table, i);
                if (entry != null) {
                    ThreadLocal threadLocal = (ThreadLocal) referentField.get(entry);
                    threadLocal.remove();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

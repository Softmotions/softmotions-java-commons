package com.softmotions.weboot.lifecycle;

import java.lang.reflect.Method;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
class LCSlot implements Comparable<LCSlot> {
    final Method method;
    final Object target;
    final int order;

    LCSlot(Method method, Object target, int order) {
        this.method = method;
        this.target = target;
        this.order = order;
    }

    public int compareTo(LCSlot o) {
        return order - o.order;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LCSlot lcSlot = (LCSlot) o;
        if (!method.equals(lcSlot.method)) return false;
        if (!target.equals(lcSlot.target)) return false;
        return true;
    }

    public int hashCode() {
        int result = method.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }
}

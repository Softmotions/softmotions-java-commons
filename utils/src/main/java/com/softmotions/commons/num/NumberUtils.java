package com.softmotions.commons.num;

/**
 * Helper {@link java.lang.Number} conversion functions/
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class NumberUtils {

    private NumberUtils() {
    }

    public long number2Long(Number n, long defval) {
        return (n != null) ? n.longValue() : defval;
    }

    public int number2Int(Number n, int defval) {
        return (n != null) ? n.intValue() : defval;
    }

    public boolean number2Boolean(Number n) {
        return (n != null) && (n.intValue() != 0);
    }

    public boolean number2Boolean(Number n, boolean defval) {
        return (n != null) ? (n.intValue() != 0) : defval;
    }
}

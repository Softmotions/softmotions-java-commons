package com.softmotions.commons.num;

/**
 * Helper {@link java.lang.Number} conversion functions/
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class NumberUtils {

    private NumberUtils() {
    }

    public static long number2Long(Number n, long defval) {
        return (n != null) ? n.longValue() : defval;
    }

    public static int number2Int(Number n, int defval) {
        return (n != null) ? n.intValue() : defval;
    }

    public static boolean number2Boolean(Number n) {
        return (n != null) && (n.intValue() != 0);
    }

    public static boolean number2Boolean(Number n, boolean defval) {
        return (n != null) ? (n.intValue() != 0) : defval;
    }
}

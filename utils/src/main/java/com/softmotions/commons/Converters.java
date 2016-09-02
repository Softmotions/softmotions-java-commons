package com.softmotions.commons;

import org.apache.commons.lang3.BooleanUtils;

import com.softmotions.commons.num.NumberUtils;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class Converters {

    private Converters() {
    }

    public static boolean toBoolean(Object val) {
        if (val == null) {
            return false;
        } else if (val instanceof Number) {
            return NumberUtils.number2Boolean((Number) val);
        } else if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue();
        } else {
            return BooleanUtils.toBoolean(val.toString());
        }
    }

    public static long number2Long(Number n, long defval) {
        return NumberUtils.number2Long(n, defval);
    }

    public static int number2Int(Number n, int defval) {
        return NumberUtils.number2Int(n, defval);
    }

    public static boolean number2Boolean(Number n) {
        return NumberUtils.number2Boolean(n);
    }

    public static boolean number2Boolean(Number n, boolean defval) {
        return NumberUtils.number2Boolean(n, defval);
    }
}

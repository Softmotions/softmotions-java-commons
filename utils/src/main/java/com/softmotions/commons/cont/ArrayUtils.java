/*
 * Copyright (c) 2006 SoftMotions
 * All Rights Reserved.
 *
 * $Id: ArrayUtil.java 6812 2008-03-07 18:08:25Z adam $
 */

package com.softmotions.commons.cont;

import org.apache.commons.collections.iterators.ArrayIterator;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ArrayUtils {

    private ArrayUtils() {
    }

    @SafeVarargs
    public static <T> boolean isAnyOf(T val, T... vals) {
        return (indexOf(vals, val) != -1);
    }

    public static <T> int indexOf(T[] array, T el) {
        for (int i = 0, l = array.length; i < l; ++i) {
            if (array[i] == el || array[i].equals(el)) {
                return i;
            }
        }
        return -1;
    }

    public static <T> T elementAt(T[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }


    /**
     * Do delimited join of generic array object. Supports java.sql.Array type
     *
     * @param array     Generic array object or java.sql.Array
     * @param delimiter Delimiter
     * @return
     */
    public static String stringJoin(Object array, String delimiter) {
        if (array == null) {
            return null;
        }
        if (array instanceof java.sql.Array) {
            try {
                array = ((java.sql.Array) array).getArray();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (array == null) {
                return null;
            }
        }

        Class clazz = array.getClass();
        if (!clazz.isArray()) {
            return String.valueOf(array);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, l = Array.getLength(array); i < l; ++i) {
            if (delimiter != null && i > 0) {
                sb.append(delimiter);
            }
            sb.append(String.valueOf(Array.get(array, i)));
        }
        return sb.toString();
    }


    public static String[] split(String str, String delims) {
        StringTokenizer st = new StringTokenizer(str, delims);
        String[] res = new String[st.countTokens()];
        for (int i = 0, l = res.length; i < l && st.hasMoreTokens(); ++i) {
            res[i] = st.nextToken();
        }
        return res;
    }


    /**
     * Returns iterator over generic array. Supports java.sql.Array type
     *
     * @param array Generic array object or java.sql.Array
     * @return
     */
    public static Iterator asArrayIterator(Object array) {
        if (array == null) {
            return Collections.emptyIterator();
        }
        if (array instanceof java.sql.Array) {
            try {
                array = ((java.sql.Array) array).getArray();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (array == null) {
                return Collections.emptyIterator();
            }
        }

        Class clazz = array.getClass();
        if (!clazz.isArray()) {
            return new ArrayIterator(new Object[]{array});
        } else {
            return new ArrayIterator(array);
        }
    }

}

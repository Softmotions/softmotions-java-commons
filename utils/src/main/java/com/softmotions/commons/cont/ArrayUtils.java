/*
 * Copyright (c) 2006 SoftMotions
 * All Rights Reserved.
 *
 * $Id: ArrayUtil.java 6812 2008-03-07 18:08:25Z adam $
 */

package com.softmotions.commons.cont;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id: ArrayUtil.java 6812 2008-03-07 18:08:25Z adam $
 */
public class ArrayUtils {

    private ArrayUtils() {
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

}

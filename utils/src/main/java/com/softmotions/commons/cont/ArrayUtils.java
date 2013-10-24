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

    public static Object getElement(Object[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }


    /**
     * Конвертирует String[] => Long[]
     *
     * @param vals
     * @return
     */
    public static Long[] toLongArray(String[] vals) {
        Long[] res = new Long[vals.length];
        for (int i = 0; i < vals.length; ++i) {
            res[i] = Long.valueOf(vals[i]);
        }
        return res;
    }
}

package com.softmotions.commons.date;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.jetbrains.annotations.Contract;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DateTimeHelper {

    public static final SimpleDateFormat DD_MM_YYYY_SDF = new SynchronizedSDF("dd.MM.yyyy");

    public static final SimpleDateFormat DD__MM__YYYY__SDF = new SynchronizedSDF("dd MMMM yyyy");
    
    public static final SimpleDateFormat HH_mm__DD__MM__YYYY__SDF = new SynchronizedSDF("HH:mm dd MMMM yyyy");

    private DateTimeHelper() {
    }

    /**
     * Truncates the given date to the beginning of a day
     */
    @Contract("null -> null")
    public static Date toStartDay(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTime();
    }


    /**
     * Truncates the given date to the and of a day
     */
    @Contract("null -> null")
    public static Date toEndOfDay(Date date) {
        if (date == null) {
            return null;
        }
        date = toStartDay(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTime();
    }
}

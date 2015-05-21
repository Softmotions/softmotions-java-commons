package com.softmotions.commons.date;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DateHelper {

    private DateHelper() {
    }

    /**
     * Truncates the given date to the beginning of a day
     */
    public static Date trunkDayDate(Date date) {
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
    public static Date trunkEndOfDay(Date date) {
        if (date == null) {
            return null;
        }
        date = trunkDayDate(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTime();
    }
}

package com.softmotions.commons.date;

import java.text.AttributedCharacterIterator;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Synchronized thread safe wrapper for SimpleDateFormat.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings({"SynchronizeOnThis"})
public final class SynchronizedSDF extends SimpleDateFormat {

    public SynchronizedSDF() {
    }

    public SynchronizedSDF(String pattern) {
        super(pattern);
    }

    public SynchronizedSDF(String pattern, Locale locale) {
        super(pattern, locale);
    }

    public SynchronizedSDF(String pattern, DateFormatSymbols formatSymbols) {
        super(pattern, formatSymbols);
    }

    @Override
    public Date parse(String source) throws ParseException {
        synchronized (this) {
            return super.parse(source);
        }
    }

    @Override
    public Date parse(String text, ParsePosition pos) {
        synchronized (this) {
            return super.parse(text, pos);
        }
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
        synchronized (this) {
            return super.format(date, toAppendTo, pos);
        }
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        synchronized (this) {
            return super.formatToCharacterIterator(obj);
        }
    }

    @Override
    public void set2DigitYearStart(Date startDate) {
        synchronized (this) {
            super.set2DigitYearStart(startDate);
        }
    }


    @Override
    public void applyPattern(String pattern) {
        synchronized (this) {
            super.applyPattern(pattern);
        }
    }

    @Override
    public void applyLocalizedPattern(String pattern) {
        synchronized (this) {
            super.applyLocalizedPattern(pattern);
        }
    }

    @Override
    public Date get2DigitYearStart() {
        synchronized (this) {
            return super.get2DigitYearStart();
        }
    }

    @Override
    public String toPattern() {
        synchronized (this) {
            return super.toPattern();
        }
    }

    @Override
    public String toLocalizedPattern() {
        synchronized (this) {
            return super.toLocalizedPattern();
        }
    }

    @Override
    public DateFormatSymbols getDateFormatSymbols() {
        synchronized (this) {
            return super.getDateFormatSymbols();
        }
    }

    @Override
    public Object parseObject(String source) throws ParseException {
        synchronized (this) {
            return super.parseObject(source);
        }
    }

    @Override
    public Object clone() {
        synchronized (this) {
            return super.clone();
        }
    }

    public int hashCode() {
        synchronized (this) {
            return super.hashCode();
        }
    }

    public boolean equals(Object obj) {
        synchronized (this) {
            return super.equals(obj);
        }
    }

    @Override
    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        synchronized (this) {
            super.setDateFormatSymbols(newFormatSymbols);
        }
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        synchronized (this) {
            return super.parseObject(source, pos);
        }
    }

    @Override
    public void setCalendar(Calendar newCalendar) {
        synchronized (this) {
            super.setCalendar(newCalendar);
        }
    }

    @Override
    public Calendar getCalendar() {
        synchronized (this) {
            return super.getCalendar();
        }
    }

    @Override
    public void setNumberFormat(NumberFormat newNumberFormat) {
        synchronized (this) {
            super.setNumberFormat(newNumberFormat);
        }
    }

    @Override
    public NumberFormat getNumberFormat() {
        synchronized (this) {
            return super.getNumberFormat();
        }
    }

    @Override
    public void setTimeZone(TimeZone zone) {
        synchronized (this) {
            super.setTimeZone(zone);
        }
    }

    @Override
    public TimeZone getTimeZone() {
        synchronized (this) {
            return super.getTimeZone();
        }
    }

    @Override
    public void setLenient(boolean lenient) {
        synchronized (this) {
            super.setLenient(lenient);
        }
    }

    @Override
    public boolean isLenient() {
        synchronized (this) {
            return super.isLenient();
        }
    }
}
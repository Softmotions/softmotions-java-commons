/*
 * Copyright (c) 2006 SoftMotions
 * All Rights Reserved.
 *
 * $Id: TimePointLogger.java 14910 2010-08-25 04:45:39Z adam $
 */

package com.softmotions.commons.perf;

import com.softmotions.commons.cont.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Утилитный класс позволяющий оценивать время выполнения
 * блоков кода в java программе. Что существенно облегчает отладку
 * программ на производительность.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 * @version $Id: TimePointLogger.java 14910 2010-08-25 04:45:39Z adam $
 */

@NotThreadSafe
public final class TimePointLogger {

    private final Logger log = LoggerFactory.getLogger(TimePointLogger.class);

    private final Map<String, TimePoint> timePointMap = new HashMap<>();

    private final Stack<TimePoint> timePoints = new Stack<>();

    private static final ThreadLocal<TimePointLogger> loggerStore = new ThreadLocal<>();

    private TimePointLogger() {
    }

    /**
     * Возвращает экземпляр TimePointLogger привязанный к текущему
     * потоку, т.е. выполняется правило:
     * только один экземпляр TimePointLogger привязан к текущему потоку выполнения.
     *
     * @return
     */
    public static TimePointLogger getInstance() {
        TimePointLogger logger = loggerStore.get();
        if (logger == null) {
            logger = new TimePointLogger();
            loggerStore.set(logger);
        }
        return logger;
    }

    /**
     * Создает точку измерения времени (timepoint)
     * с именем point
     *
     * @param point Имя точки
     */
    public void addTimePoint(String point) {
        TimePoint tp = timePointMap.get(point);
        if (tp == null) {
            tp = new TimePoint(System.currentTimeMillis(), point);
            timePointMap.put(point, tp);
        } else {
            tp.start = System.currentTimeMillis();
            tp.isFlushed = false;
        }
        timePoints.push(tp);
    }

    /**
     * Сбрасывает текущую точку измемерения времени
     * Время выполнения кода между addTimePoint и flushPoint
     * добавляется к статистике для ткущей точки выполнения.
     *
     * @return
     */
    public TimePoint flushPoint() {
        TimePoint point = timePoints.pop();
        if (point != null) {
            point.end = System.currentTimeMillis();
            point.totalTime += (point.end - point.start);
            point.isFlushed = true;
        }
        return point;
    }

    /**
     * Сбрасываем всю стат информацию по времени выполнения
     * программы между точками (timepoints) для текущего TimePointLogger
     */
    public void reset() {
        if (!timePoints.isEmpty()) {
            log.warn("Unflashed timepoints: " + timePoints);
        }
        timePoints.clear();
        timePointMap.clear();
    }

    /**
     * Выводим в виде строки отчет
     * времени  выполнения пограммы
     *
     * @param isReset Если true то после выполнения данного
     *                метода будет вызван метод {@link #reset()}
     * @return
     */
    public String printFlushedPoints(boolean isReset) {

        if (isReset && !timePoints.isEmpty()) {
            log.warn("Unflashed timepoints: " + timePoints);
            log.warn("Forced flushing unflushed timepoints");
            for (TimePoint point : timePoints) {
                point.end = System.currentTimeMillis();
                point.totalTime += (point.end - point.start);
                point.isFlushed = true;
            }
            timePoints.clear();
        }

        StringBuilder sb = new StringBuilder();
        List<TimePoint> flushed = new LinkedList<TimePoint>();
        for (final TimePoint tp : timePointMap.values()) {
            if (tp.isFlushed) {
                flushed.add(tp);
            }
        }
        Collections.sort(flushed, new Comparator<TimePoint>() {
            @Override
            public int compare(TimePoint tp1, TimePoint tp2) {
                return (tp1.totalTime > tp2.totalTime ? -1 : (tp1.totalTime == tp2.totalTime ? 0 : 1));
            }
        });
        String el = System.lineSeparator();
        for (final TimePoint tp : flushed) {
            sb.append(tp.toString());
            sb.append(el);
        }
        if (isReset) {
            reset();
        }
        return sb.toString();
    }

    public static final class TimePoint {

        private long end;
        private long start;
        private String name;
        private long totalTime;
        private boolean isFlushed;

        private TimePoint(long start, String name) {
            this.start = start;
            this.name = name;
        }

        public String toString() {
            if (end < start) {
                return "TimePoint: '" + name + "' started in=" + start;
            } else {
                return "TimePoint: '" + name + "' completed in=" + totalTime + " ms";
            }
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof TimePoint) {
                return Objects.equals(((TimePoint) o).name, name);
            }
            return false;
        }

        public int hashCode() {
            return Objects.hashCode(name);
        }
    }
}

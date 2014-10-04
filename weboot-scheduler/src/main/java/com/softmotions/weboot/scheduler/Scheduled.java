package com.softmotions.weboot.scheduler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {
    /**
     * The scheduling pattern for the task
     *
     * <p>
     * A UNIX crontab-like pattern is a string split in five space separated parts.
     * Each part is intented as:
     * </p>
     * <ol>
     * <li><strong>Minutes sub-pattern</strong>. During which minutes of the hour
     * should the task been launched? The values range is from 0 to 59.</li>
     * <li><strong>Hours sub-pattern</strong>. During which hours of the day should
     * the task been launched? The values range is from 0 to 23.</li>
     * <li><strong>Days of month sub-pattern</strong>. During which days of the
     * month should the task been launched? The values range is from 1 to 31. The
     * special value L can be used to recognize the last day of month.</li>
     * <li><strong>Months sub-pattern</strong>. During which months of the year
     * should the task been launched? The values range is from 1 (January) to 12
     * (December), otherwise this sub-pattern allows the aliases &quot;jan&quot;,
     * &quot;feb&quot;, &quot;mar&quot;, &quot;apr&quot;, &quot;may&quot;,
     * &quot;jun&quot;, &quot;jul&quot;, &quot;aug&quot;, &quot;sep&quot;,
     * &quot;oct&quot;, &quot;nov&quot; and &quot;dec&quot;.</li>
     * <li><strong>Days of week sub-pattern</strong>. During which days of the week
     * should the task been launched? The values range is from 0 (Sunday) to 6
     * (Saturday), otherwise this sub-pattern allows the aliases &quot;sun&quot;,
     * &quot;mon&quot;, &quot;tue&quot;, &quot;wed&quot;, &quot;thu&quot;,
     * &quot;fri&quot; and &quot;sat&quot;.</li>
     * </ol>
     *
     * For full description @see it.sauronsoftware.cron4j.SchedulingPattern
     */
    String pattern();
}

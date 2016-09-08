package com.softmotions.weboot.scheduler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.SchedulingPattern;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.commons.lifecycle.Dispose;
import com.softmotions.commons.lifecycle.Start;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
@Singleton
public class SchedulerModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    private Scheduler scheduler;

    private ServicesConfiguration cfg;

    public SchedulerModule() {
        this.scheduler = new Scheduler();
    }

    /**
     * Constructor for optional configuration. Configuration is used for named cron pattern
     *
     * @param cfg
     */
    public SchedulerModule(ServicesConfiguration cfg) {
        this.cfg = cfg;
        this.scheduler = new Scheduler();
    }

    @Override
    protected void configure() {
        bindListener(Matchers.any(), new ScheduledAnnotatedListener());
        bind(SchedulerModule.class).toInstance(this);
        bind(Scheduler.class).toInstance(scheduler);
        bind(SchedulerInitializer.class).asEagerSingleton();
        if ((cfg == null) || cfg.xcfg().configurationsAt("scheduler").isEmpty()) {
            log.warn("No SchedulerModule configuration found. Skipping.");
            return;
        }
    }

    private boolean hasScheduledMethod(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(Scheduled.class) != null) {
                return true;
            }
        }
        return false;
    }

    private void registerScheduled(final Object target) {
        for (final Method method : target.getClass().getMethods()) {
            Scheduled scheduled = method.getAnnotation(Scheduled.class);
            if (scheduled != null) {
                String scheduledPattern;
                if (StringUtils.isNotBlank(scheduled.patternName())) { //For named configuration
                    Map<String, String> namedTasks = new HashMap<>();

                    //
                    for (String task : cfg.xcfg().getStringArray("scheduler.named-tasks")) {
                        String[] s = task.split("=", 2);
                        if (s.length != 2) {
                            log.warn("Incorrect format for a named task, skipping: {}, {}", task, s.length);
                            continue;
                        }
                        namedTasks.put(s[0].trim(), s[1].trim());
                    }

                    scheduledPattern = namedTasks.get(scheduled.patternName());
                    if (scheduledPattern == null) {
                        if (StringUtils.isNotBlank(scheduled.value())) { //try to fall back to default pattern
                            log.info("No such named pattern found in configuration: '{}'", scheduled.patternName());
                            log.info("Falling back to a default value: '{}'", scheduled.value());
                            scheduledPattern = scheduled.value();
                        } else { //
                            log.warn("No such named pattern found in configuration, skipping: '{}'", scheduled.patternName());
                            continue;
                        }
                    }
                } else {
                    scheduledPattern = scheduled.value();
                }

                if (!SchedulingPattern.validate(scheduledPattern)) {
                    log.warn("Invalid scheduler pattern: '{}' for {}#{}",
                             scheduledPattern, target.getClass().getName(), method.getName());
                    continue;
                }

                log.info("Register scheduled task: pattern: '{}', method: {}#{}",
                         scheduledPattern, target.getClass().getName(), method.getName());

                scheduler.schedule(scheduledPattern, new Task() {
                    @Override
                    public void execute(TaskExecutionContext context) throws RuntimeException {
                        try {
                            Class<?>[] ptypes = method.getParameterTypes();
                            Object[] params = new Object[ptypes.length];
                            for (int i = 0; i < ptypes.length; ++i) {
                                Class<?> ptype = ptypes[i];
                                if (TaskExecutionContext.class.isAssignableFrom(ptype)) {
                                    params[i] = context;
                                } else {
                                    params[i] = null;
                                }
                            }
                            method.invoke(target, params);
                        } catch (Exception e) {
                            log.error("", e);
                        }
                    }
                });
            }
        }
    }

    private class ScheduledAnnotatedListener implements TypeListener {
        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            if (hasScheduledMethod(type.getRawType())) {
                encounter.register(new ScheduledListener<I>());
            }
        }
    }

    private class ScheduledListener<I> implements InjectionListener<I> {
        @Override
        public void afterInjection(final I injectee) {
            registerScheduled(injectee);
        }
    }

    public static class SchedulerInitializer {
        private final Scheduler scheduler;

        @Inject
        public SchedulerInitializer(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        @Start(order = Integer.MAX_VALUE)
        public void start() {
            log.info("Starting scheduler");
            scheduler.start();
        }

        @Dispose(order = Integer.MAX_VALUE)
        public void dispose() {
            log.info("Stopping scheduler");
            scheduler.stop();
        }
    }
}

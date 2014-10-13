package com.softmotions.weboot.scheduler;

import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.SchedulingPattern;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import com.softmotions.weboot.lifecycle.Dispose;
import com.softmotions.weboot.lifecycle.Start;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
@Singleton
public class SchedulerModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    private Scheduler scheduler;

    public SchedulerModule() {
        scheduler = new Scheduler();
    }

    protected void configure() {
        bindListener(Matchers.any(), new ScheduledAnnotatedListener());
        bind(SchedulerModule.class).toInstance(this);
        bind(Scheduler.class).toInstance(scheduler);
        bind(SchedulerInitializer.class).asEagerSingleton();
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
                if (!SchedulingPattern.validate(scheduled.value())) {
                    log.warn("Invalid scheduler pattern: '" + scheduled.value() + "' " +
                             "for " + target.getClass().getName() + "#" + method.getName());
                    continue;
                }

                log.info("Register scheduled task: " +
                         "pattern: '" + scheduled.value() + "', " +
                         "method: " + target.getClass().getName() + "#" + method.getName());

                scheduler.schedule(scheduled.value(), new Task() {
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
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            if (hasScheduledMethod(type.getRawType())) {
                encounter.register(new ScheduledListener<I>());
            }
        }
    }

    private class ScheduledListener<I> implements InjectionListener<I> {
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

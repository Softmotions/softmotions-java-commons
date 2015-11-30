package com.softmotions.weboot.lifecycle;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class LifeCycleModule extends AbstractModule {

    private final Logger log = LoggerFactory.getLogger(LifeCycleModule.class);

    final Object lock;
    final Set<LCSlot> startSet;
    final Set<LCSlot> stopSet;
    volatile boolean started;

    public LifeCycleModule() {
        this.lock = new Object();
        this.startSet = new HashSet<>();
        this.stopSet = new HashSet<>();
    }

    boolean hasLifecycleMethod(Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(Start.class) != null || method.getAnnotation(Dispose.class) != null) {
                return true;
            }
        }
        return false;
    }

    private class LifecycleAnnotatedListener implements TypeListener {
        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            if (hasLifecycleMethod(type.getRawType())) {
                // Add the listener
                encounter.register(new LifecycleListener<>());
            }
        }
    }

    private class LifecycleListener<I> implements InjectionListener<I> {
        @Override
        public void afterInjection(final I injectee) {
            registerLifecycle(injectee);
        }
    }

    void registerLifecycle(Object target) {
        for (final Method method : target.getClass().getMethods()) {
            Start start = method.getAnnotation(Start.class);
            if (start != null) {
                registerStartSlot(new LCSlot(method, target, start.order(), start.parallel()));
            }
            Dispose dispose = method.getAnnotation(Dispose.class);
            if (dispose != null) {
                registerStopSlot(new LCSlot(method, target, dispose.order(), false));
            }
        }
    }

    void registerStartSlot(LCSlot slot) {
        boolean s = started;
        if (s) {
            log.warn("Startable instantiated after the application has been started: " + slot.target.toString());
            try {
                invokeTarget(slot);
            } catch (Exception e) {
                log.error("", e);
            }
        } else {
            synchronized (lock) {
                startSet.add(slot);
            }
        }
    }

    void registerStopSlot(LCSlot slot) {
        synchronized (lock) {
            stopSet.add(slot);
        }
    }

    void invokeTarget(LCSlot slot) throws Exception {
        slot.method.invoke(slot.target);
    }

    @Override
    protected void configure() {
        bindListener(Matchers.any(), new LifecycleAnnotatedListener());
        bind(LifeCycleModule.class).toInstance(this);
        bind(LifeCycleService.class).to(LifeCycleServiceImpl.class).asEagerSingleton();
    }
}

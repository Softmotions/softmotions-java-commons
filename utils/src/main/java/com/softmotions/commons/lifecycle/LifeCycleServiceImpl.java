package com.softmotions.commons.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.internal.ProviderMethod;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import com.google.inject.spi.ProviderInstanceBinding;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class LifeCycleServiceImpl implements LifeCycleService {

    private static final Logger log = LoggerFactory.getLogger(LifeCycleServiceImpl.class);

    private final Injector injector;

    private LifeCycleModule lc;

    @Inject
    public LifeCycleServiceImpl(Injector injector, LifeCycleModule lc) {
        this.injector = injector;
        this.lc = lc;
    }

    @Override
    public void start() {
        start(true);
    }

    public void start(boolean failFast) {
        ExecutorService parallelExec = null;
        log.info("Starting");
        for (final Binding binding : injector.getBindings().values()) {
            binding.acceptScopingVisitor(new DefaultBindingScopingVisitor() {
                @Override
                public Object visitEagerSingleton() {
                    injector.getInstance(binding.getKey());
                    return null;
                }

                @Override
                public Object visitScope(Scope scope) {
                    if (scope.equals(Scopes.SINGLETON)) {
                        Object target = injector.getInstance(binding.getKey());
                        if (binding instanceof ProviderInstanceBinding) {
                            Provider providerInstance = ((ProviderInstanceBinding) binding).getProviderInstance();
                            if (providerInstance instanceof ProviderMethod) {
                                // @Provides methods don't get picked up by TypeListeners, so we need to manually register them
                                if (lc.hasLifecycleMethod(target.getClass())) {
                                    lc.registerLifecycle(target);
                                }
                            }
                        }
                    }
                    return null;
                }
            });
        }

        AtomicReference<Exception> lastErrorRef = new AtomicReference<>();
        List<LCSlot> startList;
        synchronized (lc.lock) {
            if (lc.started) {
                log.warn("Lifecycle service was started already");
                return;
            }
            lc.started = true;
            startList = new ArrayList<>(lc.startSet);
            lc.startSet.clear();
        }
        Collections.sort(startList);

        try {
            for (final LCSlot s : startList) {

                if (failFast && lastErrorRef.get() != null) {
                    break;
                }
                if (!s.parallel) {
                    try {
                        lc.invokeTarget(s);
                    } catch (Exception e) {
                        lastErrorRef.set(e);
                        log.error("", e);
                    }
                } else {
                    if (parallelExec == null) {
                        parallelExec = Executors.newFixedThreadPool(
                                Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
                        );
                    }
                    parallelExec.execute(() -> {
                        try {
                            lc.invokeTarget(s);
                        } catch (Exception e) {
                            lastErrorRef.set(e);
                            log.error("", e);
                        }
                    });
                }
            }

        } finally {
            Exception lastError = lastErrorRef.get();
            if (parallelExec != null) {
                if (failFast && lastError != null) {
                    parallelExec.shutdownNow();
                } else {
                    parallelExec.shutdown();
                }
            }
            if (failFast && lastError != null) {
                throw new RuntimeException(lastError);
            }
        }
    }


    @Override
    public void stop() {
        log.info("Shutdown");
        List<LCSlot> stopList;
        synchronized (lc.lock) {
            if (!lc.started) {
                log.warn("Lifecycle service was stopped already");
                return;
            }
            lc.started = false;
            stopList = new ArrayList<>(lc.stopSet);
            lc.stopSet.clear();
        }
        Collections.sort(stopList);
        Collections.reverse(stopList);
        for (final LCSlot s : stopList) {
            try {
                lc.invokeTarget(s);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    @Override
    public boolean isStarted() {
        return lc.started;
    }
}

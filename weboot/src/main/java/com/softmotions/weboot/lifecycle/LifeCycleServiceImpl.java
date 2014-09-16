package com.softmotions.weboot.lifecycle;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.internal.ProviderMethod;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import com.google.inject.spi.ProviderInstanceBinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class LifeCycleServiceImpl implements LifeCycleService {

    private static final Logger log = LoggerFactory.getLogger(LifeCycleServiceImpl.class);

    private final Injector injector;

    private LifeCycleModule lc;

    @Inject
    public LifeCycleServiceImpl(Injector injector, LifeCycleModule lc) {
        this.injector = injector;
        this.lc = lc;
    }

    public void start() {
        log.info("Starting");
        for (final Binding binding : injector.getBindings().values()) {
            binding.acceptScopingVisitor(new DefaultBindingScopingVisitor() {
                public Object visitEagerSingleton() {
                    injector.getInstance(binding.getKey());
                    return null;
                }

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
        for (final LCSlot s : startList) {
            try {
                lc.invokeTarget(s);
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

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

    public boolean isStarted() {
        return lc.started;
    }
}

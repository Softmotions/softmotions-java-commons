package com.softmotions.commons.ebus;

import com.google.common.eventbus.EventBus;

/**
 * Default {@link com.softmotions.commons.ebus.EBus} implementation.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DefaultEBus implements EBus {

    private final EventBus eventBus;

    public DefaultEBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }


    @Override
    public void fire(Object event) {
        eventBus.post(event);
    }

    @Override
    public void register(Object object) {
        eventBus.register(object);
    }

    @Override
    public void unregister(Object object) {
        eventBus.unregister(object);
    }
}

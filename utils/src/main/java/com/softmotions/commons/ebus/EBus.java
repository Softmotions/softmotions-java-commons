package com.softmotions.commons.ebus;

/**
 * Decorator interface of event bus
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface EBus {

    void fire(Object event);

    void register(Object object);

    void unregister(Object object);
}

package com.softmotions.commons.lifecycle;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface LifeCycleService {

    void start();

    void stop();

    boolean isStarted();
}

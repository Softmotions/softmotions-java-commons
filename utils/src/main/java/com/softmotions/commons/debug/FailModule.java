package com.softmotions.commons.debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.softmotions.commons.lifecycle.Start;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class FailModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(FailModule.class);

    @Override
    protected void configure() {
        bind(FailStart.class).asEagerSingleton();
    }

    static class FailStart {

        @Start
        public void failStart() {
            log.warn("Fail start triggered!!!");
            throw new RuntimeException("Fail start triggered!");
        }

    }
}

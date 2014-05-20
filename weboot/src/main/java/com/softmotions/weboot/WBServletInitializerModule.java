package com.softmotions.weboot;

/**
 * Guice modules registered in [modules] sections of WBConfiguration
 * implemented this interface are granted to initalize servlet components
 * in the given {@link com.softmotions.weboot.WBServletModule}
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WBServletInitializerModule {

    void initServlets(WBServletModule m);
}

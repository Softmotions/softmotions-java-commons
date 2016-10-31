package com.softmotions.weboot.security;

import org.apache.shiro.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.commons.JVMResources;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBShiroJVMObjectFactory implements Factory {

    private static final Logger log = LoggerFactory.getLogger(WBShiroJVMObjectFactory.class);

    private String resourceName;

    private Class requiredType;

    public void setResourceName(String resourceName) {
        log.info("resourceName={}", resourceName);
        this.resourceName = resourceName;
    }

    public void setRequiredType(Class requiredType) {
        log.info("requiredType={}", requiredType);
        this.requiredType = requiredType;
    }

    @Override
    public Object getInstance() {
        if (resourceName == null) {
            throw new RuntimeException("resourceName is not set");
        }
        Object res = JVMResources.getOrFail(resourceName);
        if (requiredType != null) {
            return requiredType.cast(res);
        } else {
            return res;
        }
    }
}

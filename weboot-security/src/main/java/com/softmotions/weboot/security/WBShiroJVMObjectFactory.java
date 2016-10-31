package com.softmotions.weboot.security;

import org.apache.shiro.util.Factory;

import com.softmotions.commons.JVMResources;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBShiroJVMObjectFactory implements Factory {

    private String resourceName;

    private Class requiredType;

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setRequiredType(Class requiredType) {
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

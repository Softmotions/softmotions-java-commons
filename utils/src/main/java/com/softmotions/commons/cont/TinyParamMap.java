package com.softmotions.commons.cont;

import org.apache.commons.collections.map.Flat3Map;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TinyParamMap extends Flat3Map {

    public TinyParamMap param(String k, Object v) {
        put(k, v);
        return this;
    }
}
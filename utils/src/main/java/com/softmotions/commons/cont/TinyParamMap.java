package com.softmotions.commons.cont;

import org.apache.commons.collections4.map.Flat3Map;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class TinyParamMap<V> extends Flat3Map<String, V> {

    public TinyParamMap<V> param(String k, V v) {
        put(k, v);
        return this;
    }
}
package com.softmotions.commons.weboot.mb;

import org.apache.commons.collections.map.Flat3Map;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class MBTinyParams extends Flat3Map {

    public MBTinyParams param(String k, Object v) {
        put(k, v);
        return this;
    }
}

package com.softmotions.commons.json;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Date;
import java.util.Map;

/**
 * Various JSON Utilities
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class JsonUtils {

    private JsonUtils() {
    }


    public static ObjectNode populateObjectNode(Object bean, ObjectNode o, String... keys) {
        if (bean == null) {
            return o;
        }
        if (bean instanceof Map) {
            return populateObjectNode((Map) bean, o, keys);
        }
        PropertyUtilsBean pu = BeanUtilsBean.getInstance().getPropertyUtils();
        try {
            return populateObjectNode(pu.describe(bean), o, keys);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static ObjectNode populateObjectNode(Map map, ObjectNode o, String... keys) {
        if (map == null) {
            return o;
        }
        for (final Object oe : map.entrySet()) {
            Map.Entry e = (Map.Entry) oe;
            String key = (String) e.getKey();
            Object val = e.getValue();
            if (val == null) {
                o.putNull(key);
                continue;
            }
            if (keys != null && keys.length > 0 && ArrayUtils.indexOf(keys, key) == -1) {
                continue;
            }
            if (val instanceof String) {
                o.put(key, (String) val);
            } else if (val instanceof Number) {
                if (val instanceof Float || val instanceof Double) {
                    o.put(key, ((Number) val).doubleValue());
                } else {
                    o.put(key, ((Number) val).longValue());
                }
            } else if (val instanceof Date) {
                o.put(key, ((Date) val).getTime());
            } else if (val instanceof Boolean) {
                o.put(key, (Boolean) val);
            } else if (val instanceof Map) {
                populateObjectNode((Map) val, o.putObject(key));
            } else {
                o.putPOJO(key, val);
            }
        }
        return o;
    }
}

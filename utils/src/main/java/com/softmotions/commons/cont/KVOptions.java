package com.softmotions.commons.cont;

import java.util.Map;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.Flat3Map;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.softmotions.commons.json.JsonUtils;

/**
 * Fast key->value map with support of typed values and
 * serialization/deserialization from string representation.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@SuppressWarnings("unchecked")
public class KVOptions extends Flat3Map<String, String> {

    public KVOptions() {
    }

    public KVOptions(Map map) {
        super(map);
    }

    public KVOptions(String spec) {
        loadOptions(spec);
    }

    public KVOptions with(String key, String val) {
        if (key != null && val != null) {
            put(key, val);
        }
        return this;
    }

    public boolean getBoolean(String key) {
        return BooleanUtils.toBoolean(getString(key));
    }

    public Boolean getBooleanObject(String key) {
        return BooleanUtils.toBoolean(getString(key));
    }

    public String getString(String key) {
        Object val = get(key);
        return val != null ? val.toString() : null;
    }

    public int getInt(String key, int defVal) {
        if (!containsKey(key)) {
            return defVal;
        }
        try {
            return Integer.parseInt(String.valueOf(get(key)));
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public Integer getIntObject(String key, Integer defVal) {
        if (!containsKey(key)) {
            return defVal;
        }
        try {
            return Integer.parseInt(String.valueOf(get(key)));
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public long getLong(String key, long defVal) {
        if (!containsKey(key)) {
            return defVal;
        }
        try {
            return Long.parseLong(String.valueOf(get(key)));
        } catch (NumberFormatException e) {
            return defVal;
        }
    }

    public Long getLongObject(String key, Long defVal) {
        if (!containsKey(key)) {
            return defVal;
        }
        try {
            return Long.parseLong(String.valueOf(get(key)));
        } catch (NumberFormatException e) {
            return defVal;
        }
    }


    public void loadOptions(String spec) {
        if (spec == null) {
            return;
        }
        @SuppressWarnings("MultipleVariablesInDeclaration")
        int idx, sp1 = 0, sp2 = 0;
        int len = spec.length();
        boolean escaped = false;
        String part;
        while (sp1 < len) {
            idx = spec.indexOf(',', sp1);
            if (idx == -1) {
                sp1 = len;
            } else {
                if (idx > 0 && spec.charAt(idx - 1) == '\\') { //escaped delimeter ','
                    sp1 = idx + 1;
                    escaped = true;
                    continue;
                }
                sp1 = idx;
            }
            part = spec.substring(sp2, sp1);
            ++sp1;
            sp2 = sp1;
            idx = part.indexOf('=');
            if (idx != -1 && idx < len) {
                if (escaped) {
                    put(StringUtils.replace(part.substring(0, idx).trim(), "\\,", ","),
                        StringUtils.replace(part.substring(idx + 1).trim(), "\\,", ","));
                    escaped = false;
                } else {
                    put(part.substring(0, idx).trim(),
                        part.substring(idx + 1).trim());
                }
            }
        }
    }

    private String value2String(Object val) {
        if (val == null) {
            return "";
        }
        if (val instanceof Map) {
            KVOptions nopts = new KVOptions();
            Map mval = (Map) val;
            for (final Object oe : mval.entrySet()) {
                Map.Entry e = (Entry) oe;
                nopts.put(e.getKey().toString(), e.getValue() != null ? value2String(e.getValue()) : null);
            }
            return nopts.toString();
        } else if (val instanceof ObjectNode) {
            value2String(JsonUtils.populateMapByJsonNode((ObjectNode) val, new Flat3Map<>()));
        }
        return val.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        MapIterator mit = this.mapIterator();
        for (int i = 0; mit.hasNext(); ) {
            String key = String.valueOf(mit.next());
            if (mit.getValue() == null) {
                continue;
            }
            String sval = value2String(mit.getValue());
            if (sval.isEmpty()) {
                continue;
            }
            if (i > 0) {
                sb.append(',');
            }
            sb.append(StringUtils.replace(key, ",", "\\,"));
            sb.append('=');
            sb.append(StringUtils.replace(sval, ",", "\\,"));
            ++i;
        }
        return sb.toString();
    }
}




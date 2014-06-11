package com.softmotions.commons.cont;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.Flat3Map;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * Parsed assembly options.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class KVOptions extends Flat3Map {

    public KVOptions() {
    }

    public KVOptions(Map map) {
        super(map);
    }

    public KVOptions(String spec) {
        loadOptions(spec);
    }

    public void loadOptions(String spec) {
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        MapIterator mit = this.mapIterator();
        for (int i = 0; mit.hasNext(); ) {
            String key = String.valueOf(mit.next());
            if (mit.getValue() == null) {
                continue;
            }
            String val = mit.getValue().toString();
            if (val.isEmpty()) {
                continue;
            }
            if (i > 0) {
                sb.append(',');
            }
            sb.append(StringUtils.replace(key, ",", "\\,"));
            sb.append('=');
            sb.append(StringUtils.replace(val, ",", "\\,"));
            ++i;
        }
        return sb.toString();
    }
}




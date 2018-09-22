package com.softmotions.weboot.jaxrs.ws;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class JsonWSMessage extends StringWSMessage {

    public JsonWSMessage(Object msg, ObjectMapper mapper) throws Exception {
        this(null, msg, mapper);
    }

    public JsonWSMessage(String key, Object obj, ObjectMapper mapper) throws Exception {
        super(key != null
              ? mapper.writeValueAsString(mapper.createObjectNode()
                                                  .put("key", key)
                                                  .putPOJO("data", obj))
              : mapper.writeValueAsString(obj)
        );
    }
}

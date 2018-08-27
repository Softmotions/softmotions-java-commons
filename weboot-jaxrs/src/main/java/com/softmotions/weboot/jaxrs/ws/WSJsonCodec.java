package com.softmotions.weboot.jaxrs.ws;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.softmotions.commons.JVMResources;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class WSJsonCodec implements Encoder.TextStream<Object>, Decoder.Text<JsonNode> {

    private ObjectMapper _mapper;

    @Override
    public void init(EndpointConfig endpointConfig) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean willDecode(String s) {
        try {
            JsonParser parser = getMapper().getFactory().createParser(s);
            while (parser.nextToken() != null) parser.nextToken();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public JsonNode decode(String s) throws DecodeException {
        try {
            return _mapper.readTree(s);
        } catch (IOException e) {
            throw new DecodeException(s, e.getMessage(), e);
        }
    }

    @Override
    public void encode(Object object, Writer writer) throws EncodeException, IOException {
        try {
            getMapper().writeValue(writer, object);
        } catch (JsonGenerationException | JsonMappingException e) {
            throw new EncodeException(object, e.getMessage(), e);
        }
    }

    private ObjectMapper getMapper() {
        ObjectMapper m = _mapper;
        if (m == null) {
            synchronized (WSJsonCodec.class) {
                m = _mapper;
                if (m == null) {
                    Injector injector = JVMResources.getWait(
                            "com.softmotions.weboot.WBServletListener.Injector",
                            1, TimeUnit.MINUTES);
                    _mapper = injector.getInstance(ObjectMapper.class);
                    m = _mapper;
                }
            }
        }
        return m;

    }
}

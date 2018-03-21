package com.softmotions.weboot.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@SuppressWarnings("unchecked")
public class JsonNodeReader implements MessageBodyReader {

    @Inject
    private ObjectMapper mapper;

    @Override
    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return (type == JsonNode.class || type == ArrayNode.class || type == ObjectNode.class);
    }

    @Override
    public Object readFrom(Class type, Type genericType, Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap httpHeaders,
                           InputStream entityStream) throws IOException, WebApplicationException {
        JsonNode res = mapper.readTree(entityStream);
        //noinspection ObjectEquality
        if (type != JsonNode.class && !type.isAssignableFrom(res.getClass())) {
            throw new BadRequestException("Cannot map data to the required: " + type.getClass().getName());
        }
        return res;
    }
}

package com.softmotions.weboot.jaxrs.ws;

import java.nio.ByteBuffer;
import javax.websocket.Session;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public interface WSRequestContext extends WSContext {

    ObjectNode getRequestData();

    Session getSession();

    void sendError(String msg);

    void send(Object msg);

    void ping();

    void ping(ByteBuffer msg);
}

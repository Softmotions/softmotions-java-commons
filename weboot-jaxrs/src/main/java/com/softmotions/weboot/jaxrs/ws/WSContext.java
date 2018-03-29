package com.softmotions.weboot.jaxrs.ws;

import java.util.Set;
import javax.websocket.Session;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public interface WSContext {

    Set<Session> getAllSessions();

    void sendToAll(String text);

    void sendToAll(JsonNode node);

}

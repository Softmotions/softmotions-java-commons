package com.softmotions.weboot.jaxrs.ws;

import java.util.Set;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public interface WSContext {

    Set<Session> getAllSessions();

    void sendToAll(String text);

    void sendToAllAsJSON(Object data);

    void sendToAllAsJSON(String key, Object data);

}

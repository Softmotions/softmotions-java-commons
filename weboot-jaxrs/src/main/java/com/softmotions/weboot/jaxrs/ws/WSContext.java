package com.softmotions.weboot.jaxrs.ws;

import java.util.Set;
import java.util.concurrent.Executor;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public interface WSContext {

    String WS_SUBJECT_PROP_KEY = "WSubject";

    Set<Session> getAllSessions();

    void sendToAll(Object msg);

    void sendTo(Object msg, Session sess);
}

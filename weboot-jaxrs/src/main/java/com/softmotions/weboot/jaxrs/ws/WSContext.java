package com.softmotions.weboot.jaxrs.ws;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public interface WSContext {

    String WS_SUBJECT_PROP_KEY = "WSubject";

    Set<Session> getAllSessions();

    void sendToAll(String text);

    void sendToAllAsJSON(Object data);

    void sendToAllAsJSON(String key, Object data);

    CompletableFuture sendToAsync(Session sess, String text);

    Executor getExecutor();
}

package com.softmotions.weboot.jaxrs.ws;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.websocket.SendResult;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public interface WSContext {

    String WS_SUBJECT_PROP_KEY = "WSubject";

    Set<Session> getAllSessions();

    CompletableFuture<Void> sendToAll(Object msg);

    CompletableFuture<SendResult> sendTo(Object msg, Session sess);

    CompletableFuture<Void> pingAll(ByteBuffer msg);

    CompletableFuture<SendResult> ping(ByteBuffer msg, Session sess);

    CompletableFuture<Void> pingAll();

    CompletableFuture<SendResult> ping(Session sess);

    void tagSession(String tag, Session sess);

    void untagSession(String tag, Session sess);

    Set<Session> getTaggedSessions(String tag);
}

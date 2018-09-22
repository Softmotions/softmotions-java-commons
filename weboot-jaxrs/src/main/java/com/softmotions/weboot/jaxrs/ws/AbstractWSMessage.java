package com.softmotions.weboot.jaxrs.ws;

import java.util.concurrent.CompletableFuture;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public abstract class AbstractWSMessage implements WSMessage {

    private CompletableFuture<SendResult> future = new CompletableFuture<>();

    protected abstract void send(Session session, SendHandler handler);

    @Override
    public CompletableFuture<SendResult> getCompletionFuture() {
        return future;
    }

    protected void complete(SendResult result) {
        if (result.getException() != null) {
            future.completeExceptionally(result.getException());
        } else {
            future.complete(result);
        }
    }
}

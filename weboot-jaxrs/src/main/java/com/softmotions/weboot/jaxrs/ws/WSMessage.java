package com.softmotions.weboot.jaxrs.ws;

import java.util.concurrent.CompletableFuture;
import javax.websocket.SendResult;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public interface WSMessage {

    long getDataLength();

    CompletableFuture<SendResult> getCompletionFuture();
}

package com.softmotions.weboot.jaxrs.ws;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class CloseWSMessage extends AbstractWSMessage {

    @Override
    public long getDataLength() {
        return 0;
    }

    @Override
    public void send(Session session, SendHandler handler) {
        if (session.isOpen()) {
            ForkJoinPool.commonPool().submit(() -> {
                try {
                    session.close();
                } catch (IOException ignored) {
                } finally {
                    complete(new SendResult());
                }
            });
        }
    }
}

package com.softmotions.weboot.jaxrs.ws;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ForkJoinPool;
import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;

public class PingWSMessage extends AbstractWSMessage {

    private final ByteBuffer data;

    public PingWSMessage(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public long getDataLength() {
        return data.capacity();
    }

    @Override
    public void send(Session session, SendHandler handler) {
        ForkJoinPool.commonPool().submit(() -> {
            try {
                session.getBasicRemote().sendPing(data);
            } catch (IOException ignored) {
            } finally {
                SendResult result = new SendResult();
                complete(result);
                handler.onResult(result);
            }
        });
    }
}

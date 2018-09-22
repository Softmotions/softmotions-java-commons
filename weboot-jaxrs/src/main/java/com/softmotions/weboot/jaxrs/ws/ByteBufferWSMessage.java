package com.softmotions.weboot.jaxrs.ws;

import java.nio.ByteBuffer;
import javax.websocket.SendHandler;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class ByteBufferWSMessage extends AbstractWSMessage {

    private final ByteBuffer data;

    public ByteBufferWSMessage(ByteBuffer data) {
        this.data = data;
    }

    @Override
    public long getDataLength() {
        return data.capacity();
    }

    @Override
    public void send(Session session, SendHandler handler) {
        session.getAsyncRemote().sendBinary(data, result -> {
            complete(result);
            handler.onResult(result);
        });
    }
}

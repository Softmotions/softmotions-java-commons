package com.softmotions.weboot.jaxrs.ws;

import java.nio.charset.StandardCharsets;
import javax.websocket.SendHandler;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class StringWSMessage implements WSMessage {

    private final String data;

    private final long length;

    public StringWSMessage(String data) {
        this.data = data;
        this.length = data.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public long getDataLength() {
        return length;
    }

    @Override
    public void send(Session session, SendHandler handler) {
        session.getAsyncRemote().sendText(data, handler);
    }
}

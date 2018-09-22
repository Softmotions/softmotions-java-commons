package com.softmotions.weboot.jaxrs.ws;

import java.io.IOException;
import javax.websocket.SendHandler;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class CloseWSMessage implements WSMessage {

    @Override
    public long getDataLength() {
        return 0;
    }

    @Override
    public void send(Session session, SendHandler handler) {
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }
    }
}

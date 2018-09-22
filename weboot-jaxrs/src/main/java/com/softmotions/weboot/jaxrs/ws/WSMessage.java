package com.softmotions.weboot.jaxrs.ws;

import java.io.IOException;
import javax.websocket.SendHandler;
import javax.websocket.Session;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public interface WSMessage {

    long getDataLength();

    void send(Session session, SendHandler handler);
}

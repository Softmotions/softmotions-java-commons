package com.softmotions.weboot.jaxrs.ws;

import java.util.concurrent.TimeUnit;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.shiro.SecurityUtils;

import com.google.inject.Injector;
import com.softmotions.commons.JVMResources;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class WSGuiceConfigurator extends ServerEndpointConfig.Configurator {

    @Override
    public <T> T getEndpointInstance(Class<T> clazz) throws InstantiationException {
        Injector injector = JVMResources.getWait(
                "com.softmotions.weboot.WBServletListener.Injector",
                1, TimeUnit.MINUTES);
        return injector.getInstance(clazz);
    }

    @Override
    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        super.modifyHandshake(sec, request, response);
        sec.getUserProperties().put(WSContext.WS_SUBJECT_PROP_KEY, SecurityUtils.getSubject());
    }
}

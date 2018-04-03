package com.softmotions.weboot.jaxrs.ws;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.softmotions.commons.ClassUtils;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class AbstractWS implements WSContext {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final AtomicReference<Set<Session>> sessionHolder = new AtomicReference<>();

    private final Map<String, List<WSHNode>> handlers = new ConcurrentHashMap<>();

    private final ObjectMapper mapper;

    protected AbstractWS(ObjectMapper mapper, Set<WSHandler> hset) {
        this.mapper = mapper;
        for (WSHandler h : hset) {
            Arrays.stream(h.getClass().getMethods())
                  .filter(m -> {
                      Class<?>[] ptypes = m.getParameterTypes();
                      return ptypes.length == 1
                             && ptypes[0] == WSRequestContext.class
                             && ClassUtils.getAnnotation(m, WSAction.class) != null;
                  })
                  .forEach(m -> {
                      WSAction annotation = ClassUtils.getAnnotation(m, WSAction.class);
                      handlers.computeIfAbsent(annotation.value(), s -> new ArrayList<>())
                              .add(new WSHNode(annotation, h, m));
                  });
        }
    }

    @OnMessage
    public void handleMessage(Session session, JsonNode request) {
        String key = StringUtils.trimToEmpty(request.path("key").asText());
        List<WSHNode> hlist = handlers.get(key);
        if (hlist == null || hlist.isEmpty()) {
            log.error("Key '{}' not registered, request: {}", key, request);
            return;
        }
        hlist.forEach(h -> {
            try {
                Object res =
                        h.method.invoke(h.handler,
                                        new WSRequestContextImpl(session, (ObjectNode) request));
                if (res != null) {
                    ObjectNode n = mapper.createObjectNode();
                    n.put("key", h.action.key().isEmpty() ? key : h.action.key());
                    n.putPOJO("data", res);
                    synchronized (session) {
                        session.getBasicRemote().sendText(mapper.writeValueAsString(n));
                    }
                }
            } catch (Exception e) {
                log.error("", e);
            }
        });
    }

    protected JsonNode error(String msg) {
        return mapper.createObjectNode().put("key", "error").put("data", msg);
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        Set<Session> openSessions = session.getOpenSessions();
        if (!openSessions.contains(session)) {
            openSessions = new HashSet<>(openSessions);
            openSessions.add(session);
        }
        sessionHolder.set(openSessions);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        Set<Session> openSessions = session.getOpenSessions();
        if (openSessions.contains(session)) {
            openSessions = new HashSet<>(openSessions);
            openSessions.remove(session);
        }
        sessionHolder.set(session.getOpenSessions());
    }

    @OnError
    public void onError(Session session, Throwable ex) {
        log.error("error: {}", ex.getMessage(), ex);
    }

    @Override
    public Set<Session> getAllSessions() {
        Set<Session> sessions = sessionHolder.get();
        return sessions != null ? sessions : Collections.emptySet();
    }

    protected void sendToAll(String text, Set<Session> sessions) {
        for (Session session : sessions) {
            try {
                synchronized (session) {
                    session.getAsyncRemote().sendText(text);
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    protected void sendToAllAsJSON(Object node, Set<Session> sessions) {
        String buf;
        try {
            buf = mapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            log.error("", e);
            throw new RuntimeException(e);
        }
        sendToAll(buf, sessions);
    }

    protected void sendToAllAsJSON(String key, Object data, Set<Session> sessions) {
        ObjectNode n = mapper.createObjectNode();
        n.put("key", key);
        n.putPOJO("data", data);
        sendToAllAsJSON(n, sessions);
    }

    @Override
    public void sendToAll(String text) {
        sendToAll(text, getAllSessions());
    }

    @Override
    public void sendToAllAsJSON(Object data) {
        sendToAllAsJSON(data, getAllSessions());
    }

    @Override
    public void sendToAllAsJSON(String key, Object data) {
        sendToAllAsJSON(key, data, getAllSessions());
    }

    private class WSRequestContextImpl implements WSRequestContext {

        private final Session session;

        private final ObjectNode request;

        @Override
        public ObjectNode getRequestData() {
            return request;
        }

        @Override
        public Session getSession() {
            return session;
        }

        @Override
        public void sendError(String msg) throws IOException {
            sendAsJSON(error(msg));
        }

        @Override
        public Set<Session> getAllSessions() {
            return getSession().getOpenSessions();
        }

        @Override
        public void send(String text) throws IOException {
            synchronized (session) {
                session.getBasicRemote().sendText(text);
            }
        }

        @Override
        public void sendAsJSON(Object data) throws IOException {
            send(mapper.writeValueAsString(data));
        }

        @Override
        public void sendToAll(String text) {
            AbstractWS.this.sendToAll(text, getAllSessions());
        }

        @Override
        public void sendToAllAsJSON(Object data) {
            AbstractWS.this.sendToAllAsJSON(data, getAllSessions());
        }

        @Override
        public void sendToAllAsJSON(String key, Object data) {
            AbstractWS.this.sendToAllAsJSON(key, data, getAllSessions());
        }

        private WSRequestContextImpl(Session session, ObjectNode request) {
            this.session = session;
            this.request = request;
        }
    }

    private class WSHNode {
        final WSHandler handler;
        final Method method;
        final WSAction action;

        WSHNode(WSAction action, WSHandler handler, Method method) {
            log.info("Registering handler: {}::{}#{}()", action.value(), handler.getClass().getName(), method.getName());
            this.handler = handler;
            this.method = method;
            this.action = action;
        }
    }

}

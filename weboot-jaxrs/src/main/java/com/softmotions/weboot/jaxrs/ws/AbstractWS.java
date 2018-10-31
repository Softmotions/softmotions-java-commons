package com.softmotions.weboot.jaxrs.ws;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.SendResult;
import javax.websocket.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.subject.Subject;
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

    private final Map<Session, AsyncSessionWrapper> sessions = new ConcurrentHashMap<>();

    private final Map<String, List<WSHNode>> handlers = new ConcurrentHashMap<>();

    protected final ObjectMapper mapper;

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
        Object sobj = session.getUserProperties().get(WS_SUBJECT_PROP_KEY);
        if (sobj instanceof Subject) {
            ((Subject) sobj).associateWith(() -> handleMessageImpl(session, request)).run();
        } else {
            handleMessageImpl(session, request);
        }
    }

    public void handleMessageImpl(Session session, JsonNode request) {
        String key = StringUtils.trimToEmpty(request.path("key").asText());
        List<WSHNode> hlist = handlers.get(key);
        if (hlist == null || hlist.isEmpty()) {
            log.error("Key '{}' not registered, request: {}", key, request);
            return;
        }
        hlist.forEach(h -> {
            WSRequestContext wctx = new WSRequestContextImpl(session, (ObjectNode) request);
            try {
                Object res = h.method.invoke(h.handler, wctx);
                if (res != null) {
                    ObjectNode n = mapper.createObjectNode();
                    n.put("key", h.action.key().isEmpty() ? key : h.action.key());
                    n.putPOJO("data", res);
                    onWSHandlerResponse(n);
                    sendTo(mapper.writeValueAsString(n), session);
                }
            } catch (Exception e) {
                onWSHandlerException(wctx, h.action.key().isEmpty() ? key : h.action.key(), h, e);
            }
        });
    }

    protected void onWSHandlerResponse(ObjectNode n) {
    }

    protected void onWSHandlerException(WSRequestContext wctx, String key, WSHNode node, Throwable e) {
        log.error("", e);
    }

    protected JsonNode error(String msg) {
        return mapper.createObjectNode().put("key", "error").put("data", msg);
    }

    @Override
    public void tagSession(String tag, Session sess) {
        var sw = sessions.get(sess);
        if (sw != null) {
            sw.tags.add(tag);
        }
    }

    @Override
    public void untagSession(String tag, Session sess) {
        var sw = sessions.get(sess);
        if (sw != null) {
            sw.tags.remove(tag);
        }
    }

    @Override
    public Set<Session> getTaggedSessions(String tag) {
        return sessions.values().stream()
                .filter(sw -> sw.tags.contains(tag))
                .map(AsyncSessionWrapper::getSession)
                .collect(Collectors.toSet());
    }

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        sessions.computeIfAbsent(session, s -> new AsyncSessionWrapper(s, mapper));
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        sessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable ex) {
        log.error("error: {}", ex.getMessage(), ex);
    }

    @Override
    public Set<Session> getAllSessions() {
        return sessions.keySet();
    }

    @Override
    public CompletableFuture<Void> sendToAll(Object msg) {
        return sendToAll(msg, getAllSessions());
    }

    @Override
    public CompletableFuture<SendResult> sendTo(Object msg, Session sess) {
        AsyncSessionWrapper sw = sessions.get(sess);
        if (sw != null) {
            return sw.send(msg);
        } else {
            return CompletableFuture.failedFuture(new Exception("Unknown session"));
        }
    }

    protected CompletableFuture<Void> sendToAll(Object msg, Set<Session> sess) {
        if (!(msg instanceof WSMessage)
            && !(msg instanceof String)
            && !(msg instanceof Number)
            && !(msg instanceof ByteBuffer)) {
            try {
                msg = mapper.writeValueAsString(msg);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        List<CompletableFuture<SendResult>> all = new ArrayList<>(sess.size());
        for (Session session : sess.toArray(new Session[0])) {
            try {
                CompletableFuture<SendResult> f = sendTo(msg, session);
                if (!f.isCompletedExceptionally() || !f.isCancelled()) {
                    all.add(f);
                }
            } catch (Exception e) {
                log.error("", e);
            }
        }
        if (!all.isEmpty()) {
            return CompletableFuture.allOf(all.toArray(new CompletableFuture[0]));
        } else {
            return CompletableFuture.completedFuture(null);
        }
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
        public Set<Session> getAllSessions() {
            return getSession().getOpenSessions();
        }

        @Override
        public CompletableFuture<Void> sendToAll(Object msg) {
            return AbstractWS.this.sendToAll(msg);
        }

        @Override
        public void tagSession(String tag, Session sess) {
            AbstractWS.this.tagSession(tag, sess);
        }

        @Override
        public void untagSession(String tag, Session sess) {
            AbstractWS.this.untagSession(tag, sess);
        }

        @Override
        public Set<Session> getTaggedSessions(String tag) {
            return AbstractWS.this.getTaggedSessions(tag);
        }

        @Override
        public CompletableFuture<SendResult> sendTo(Object msg, Session sess) {
            return AbstractWS.this.sendTo(msg, sess);
        }

        @Override
        public void send(Object msg) {
            sendTo(msg, session);
        }

        @Override
        public void sendError(String msg) {
            try {
                send(mapper.writeValueAsString(error(msg)));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        private WSRequestContextImpl(Session session, ObjectNode request) {
            this.session = session;
            this.request = request;
        }
    }

    protected class WSHNode {
        public final WSHandler handler;
        public final Method method;
        public final WSAction action;

        WSHNode(WSAction action, WSHandler handler, Method method) {
            log.info("Registering handler: {}::{}#{}()", action.value(), handler.getClass().getName(), method.getName());
            this.handler = handler;
            this.method = method;
            this.action = action;
        }


    }
}

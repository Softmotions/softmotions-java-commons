package com.softmotions.weboot.jaxrs.ws;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import javax.websocket.CloseReason;
import javax.websocket.Session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public final class AsyncSessionWrapper {

    public static final int DEFAULT_MAX_PENDING_MESSAGES = 1024;

    public static final int DEFAULT_MAX_PENDING_BYTES = 1048576;

    private final Session sess;

    private final ObjectMapper mapper;

    private final LinkedList<WSMessage> queue = new LinkedList<>();

    private volatile boolean isSending;

    private volatile boolean isClosing;

    private AtomicLong bytesToSend = new AtomicLong(0);

    private long maxPendingMessages = DEFAULT_MAX_PENDING_MESSAGES;

    private long maxPendingBytes = DEFAULT_MAX_PENDING_BYTES;

    public AsyncSessionWrapper(Session sess, ObjectMapper mapper) {
        this.sess = sess;
        this.mapper = mapper;
    }

    void close() {
        send(new CloseWSMessage());
    }

    void send(Object msg) {
        synchronized (queue) {
            if (!sess.isOpen() || isClosing) {
                return;
            }
            if (msg instanceof CloseWSMessage) {
                isClosing = true;
            }
            if (isSending) {
                if (queue.size() > maxPendingMessages || bytesToSend.get() > maxPendingBytes) {
                    isClosing = true;
                    closeAsync(CloseReason.CloseCodes.VIOLATED_POLICY, "Pending messages/bytes exceeded limits");
                } else {
                    WSMessage wmsg = toWSMessage(msg);
                    queue.add(wmsg);
                    bytesToSend.addAndGet(wmsg.getDataLength());
                }
            } else {
                isSending = true;
                sendAsync(msg);
            }
        }
    }

    private void sendAsync(Object msg) {
        toWSMessage(msg).send(sess, res -> {
            if (!res.isOK()) {
                closeAsync(CloseReason.CloseCodes.CLOSED_ABNORMALLY,
                           res.getException() != null ? res.getException().getMessage() : "");
            }
            if (!queue.isEmpty()) {
                WSMessage wm = queue.remove();
                bytesToSend.addAndGet(-wm.getDataLength());
                sendAsync(wm);
            } else {
                isSending = false;
            }
        });
    }

    private WSMessage toWSMessage(Object msg) {
        if (msg instanceof WSMessage) {
            return (WSMessage) msg;
        } else if (msg instanceof String || msg instanceof Number) {
            return new StringWSMessage(String.valueOf(msg));
        } else if (msg instanceof JsonNode) {
            try {
                return new JsonWSMessage(msg, mapper);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (msg instanceof ByteBuffer) {
            return new ByteBufferWSMessage((ByteBuffer) msg);
        } else {
            throw new IllegalArgumentException("Unsupported message type");
        }
    }

    private void closeAsync(CloseReason.CloseCode code, String msg) {
        ForkJoinPool.commonPool().submit(() -> {
            try {
                sess.close(new CloseReason(
                        CloseReason.CloseCodes.CLOSED_ABNORMALLY,
                        msg == null ? "" : msg));
            } catch (IOException ignored) {
            }
        });
    }
}

package com.softmotions.weboot.jaxrs.ws;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.websocket.CloseReason;
import javax.websocket.SendResult;
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

    private final AtomicLong bytesToSend = new AtomicLong(0);

    private long maxPendingMessages = DEFAULT_MAX_PENDING_MESSAGES;

    private long maxPendingBytes = DEFAULT_MAX_PENDING_BYTES;

    public AsyncSessionWrapper(Session sess, ObjectMapper mapper) {
        this.sess = sess;
        this.mapper = mapper;
    }

    @Nullable
    public CompletableFuture<SendResult> close() {
        return send(new CloseWSMessage());
    }

    @Nullable
    public CompletableFuture<SendResult> send(Object msg) {
        synchronized (queue) {
            if (!sess.isOpen() || isClosing) {
                return sessionClosedFuture();
            }
            if (msg instanceof CloseWSMessage) {
                isClosing = true;
            }
            if (isSending) {
                if (queue.size() > maxPendingMessages || bytesToSend.get() > maxPendingBytes) {
                    isClosing = true;
                    String err =
                            String.format("Pending messages/bytes exceeded limits: " +
                                          "queue size: %d pending bytes: %d",
                                          queue.size(), bytesToSend.get());
                    closeAsync(CloseReason.CloseCodes.VIOLATED_POLICY, err);
                    return CompletableFuture.failedFuture(new Exception(err));
                } else {
                    WSMessage wm = toWSMessage(msg);
                    queue.add(wm);
                    bytesToSend.addAndGet(wm.getDataLength());
                    return wm.getCompletionFuture();
                }
            } else {
                isSending = true;
            }
        }
        return sendAsync(msg);
    }

    @Nullable
    private CompletableFuture<SendResult> sendAsync(Object msg) {
        if (!sess.isOpen()) {
            synchronized (queue) {
                isClosing = true;
                return sessionClosedFuture();
            }
        }
        AbstractWSMessage wm = toWSMessage(msg);
        long wmlen = wm.getDataLength();
        wm.send(sess, res -> {
            synchronized (queue) {
                bytesToSend.addAndGet(-wmlen);
                if (!res.isOK()) {
                    isClosing = true;
                    closeAsync(CloseReason.CloseCodes.CLOSED_ABNORMALLY,
                               res.getException() != null ? res.getException().getMessage() : "");
                    return;
                }
                if (!queue.isEmpty()) {
                    sendAsync(queue.remove());
                } else {
                    isSending = false;
                }
            }
        });
        return wm.getCompletionFuture();
    }

    @Nonnull
    private AbstractWSMessage toWSMessage(Object msg) {
        if (msg == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        if (msg instanceof AbstractWSMessage) {
            return (AbstractWSMessage) msg;
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
            throw new IllegalArgumentException("Unsupported message type: " + msg.getClass());
        }
    }

    private CompletableFuture<SendResult> sessionClosedFuture() {
        return CompletableFuture.failedFuture(new Exception("Session closed"));
    }

    private void closeAsync(CloseReason.CloseCode code, String msg) {
        ForkJoinPool.commonPool().submit(() -> {
            try {
                sess.close(new CloseReason(code, msg == null ? "" : msg));
            } catch (IOException ignored) {
            }
        });
    }
}
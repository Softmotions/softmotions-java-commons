package com.softmotions.weboot.jaxrs;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

import com.softmotions.commons.cont.Pair;
import com.softmotions.weboot.i18n.I18n;

/**
 * Exception used to generate client messages
 * encoded in `X-[App]-Msg-*` and `X-[App]-Err-*` HTTP headers
 * as response from JAX-RS REST methods.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class MessageException extends RuntimeException {

    @SuppressWarnings("StaticNonFinalField")
    public static volatile String APP_ID = "App";

    private boolean hasError;

    private final List<Pair<String, Boolean>> messages = new ArrayList<>();

    private Object request;

    public MessageException() {
    }

    public MessageException(String message) {
        this(message, false);
    }

    public MessageException(String message, boolean err) {
        super(message);
        addMessage(message, err);
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
        addMessage(message, true);
    }

    public MessageException(String message, Object request) {
        this(message, false, request);
    }

    public MessageException(String message, boolean err, Object request) {
        super(message);
        this.request = request;
        addMessage(message, err);
    }

    public MessageException(String message, Throwable cause, Object request) {
        super(message, cause);
        this.request = request;
        addMessage(message, true);
    }

    public MessageException(Throwable cause) {
        super(cause);
        addMessage(cause.getMessage(), true);
    }

    public String getAppId() {
        return APP_ID;
    }

    public Object getRequest() {
        return request;
    }

    public MessageException addMessage(String message, boolean err) {
        messages.add(new Pair<>(message, err));
        if (err) {
            hasError = true;
        }
        return this;
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public List<Pair<String, Boolean>> getMessages() {
        return messages;
    }

    public boolean hasErrorMessages() {
        return hasError;
    }

    public List<String> getErrorMessages() {
        List<String> msgs = new ArrayList<>(messages.size());
        for (Pair<String, Boolean> p : messages) {
            if (p.getTwo()) {
                msgs.add(p.getOne());
            }
        }
        return msgs;
    }

    public List<String> getRegularMessages() {
        List<String> msgs = new ArrayList<>(messages.size());
        for (Pair<String, Boolean> p : messages) {
            if (!p.getTwo()) {
                msgs.add(p.getOne());
            }
        }
        return msgs;
    }

    public Response.ResponseBuilder inject(Response.ResponseBuilder rb, I18n i18n) {
        return new Messages(this, i18n).inject(rb);
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageException{");
        sb.append("cause=").append(getMessage());
        sb.append(", messages=").append(messages);
        sb.append('}');
        return sb.toString();
    }

}

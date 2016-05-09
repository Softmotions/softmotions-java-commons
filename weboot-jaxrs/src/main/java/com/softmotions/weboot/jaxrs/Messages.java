package com.softmotions.weboot.jaxrs;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.MissingResourceException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.weboot.i18n.I18n;

/**
 * App messages encoded in a response headers.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class Messages {

    private static final Logger log = LoggerFactory.getLogger(Messages.class);

    public static final int MAX_MSG_LEN = 2048;

    private final MessageException messages;

    private final I18n i18n;

    public Messages(MessageException messages, I18n i18n) {
        this.messages = messages;
        this.i18n = i18n;
    }

    public static String toHeaderMsg(String msg) {
        try {
            String str = StringUtils.left(URLEncoder.encode(msg, "UTF-8"), MAX_MSG_LEN);
            if (str.endsWith("%")) {  //todo review!!!
                str = str.substring(0, str.length() - 1);
            }
            return str;
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        return "";
    }

    public Response.ResponseBuilder inject(Response.ResponseBuilder rb) {
        if (messages.isEmpty()) {
            return rb;
        }
        if (messages.hasErrorMessages()) {
            rb.status(Response.Status.INTERNAL_SERVER_ERROR);
        } else {
            rb.status(Response.Status.OK);
        }
        List<String> mlist = messages.getErrorMessages();
        for (int i = 0, l = mlist.size(); i < l; ++i) {
            rb.header("X-" + messages.getAppId() + "-Err" + i, toHeaderMsg(toLocaleMsg(mlist.get(i))));
        }
        mlist = messages.getRegularMessages();
        for (int i = 0, l = mlist.size(); i < l; ++i) {
            rb.header("X-" + messages.getAppId() + "-Msg" + i, toHeaderMsg(toLocaleMsg(mlist.get(i))));
        }
        if (messages.getCause() != null) {
            log.error(messages.getMessage(), messages);
        }
        return rb;
    }

    private String toLocaleMsg(String msg) {
        try {
            return i18n.get(msg);
        } catch (MissingResourceException ignored) {
            return msg;
        }
    }
}

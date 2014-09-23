package com.softmotions.web;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class HttpUtils {

    private HttpUtils() {
    }

    public static String toAbsoluteUrl(HttpServletRequest req) {
        return toAbsoluteUrl(req, null, null);
    }

    public static String toAbsoluteUrl(HttpServletRequest req,
                                       String overrideServerName,
                                       Integer overrideServerPort) {
        String u = req.getScheme() +
                   "://" + ((overrideServerName != null) ? overrideServerName : req.getServerName()) +
                   ":" + ((overrideServerPort != null) ? overrideServerPort : req.getServerPort()) +
                   req.getRequestURI();
        if (req.getQueryString() != null) {
            u += '?' + req.getQueryString();
        }
        return u;
    }
}

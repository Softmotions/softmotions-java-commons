package com.softmotions.web;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class HttpUtils {

    private HttpUtils() {
    }

    public static String toAbsoluteUrl(HttpServletRequest req) {
        String u = req.getScheme() +
                   "://" + req.getServerName() +
                   ":" + req.getServerPort() +
                   req.getRequestURI();
        if (req.getQueryString() != null) {
            u += '?' + req.getQueryString();
        }
        return u;
    }
}

package com.softmotions.web;

import java.net.Inet4Address;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class HttpUtils {

    private HttpUtils() {
    }

    @Nullable
    public static Cookie findCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (final Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c;
            }
        }
        return null;
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

    // See https://en.wikipedia.org/wiki/X-Forwarded-For
    // Taken from https://r.va.gg/2011/07/handling-x-forwarded-for-in-java-and-tomcat.html

    private static final Pattern IP_ADDRESS_PATTERN =
            Pattern.compile("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})");

    private static final Pattern PRIVATE_IP_ADDRESS_PATTERN =
            Pattern.compile("(^127\\.0\\.0\\.1)|(^10\\.)|(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|(^192\\.168\\.)");

    // todo not supported IPv6
    private static String findNonPrivateIpAddress(String s) {
        Matcher matcher = IP_ADDRESS_PATTERN.matcher(s);
        while (matcher.find()) {
            if (!PRIVATE_IP_ADDRESS_PATTERN.matcher(matcher.group(0)).find())
                return matcher.group(0);
            matcher.region(matcher.end(), s.length());
        }
        return null;
    }

    /**
     * Get remote address from request.
     * Note: IPv6 not supported.
     */
    public static String getAddressFromRequest(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null && (forwardedFor = findNonPrivateIpAddress(forwardedFor)) != null) {
            return forwardedFor;
        }
        return req.getRemoteAddr();
    }

    /**
     * Get remote hostname from request.
     * Note: IPv6 not supported.
     */
    public static String getHostnameFromRequest(HttpServletRequest request) {
        String addr = getAddressFromRequest(request);
        try {
            return Inet4Address.getByName(addr).getHostName();
        } catch (Exception ignored) {
        }
        return addr;
    }
}

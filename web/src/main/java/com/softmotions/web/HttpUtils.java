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

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isMobile(String ua) {
        return ua != null && (isAndroidMobile(ua) || isIphone(ua) || isIeMobile(ua));
    }

    public static boolean isMobile(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isMobile(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isTablet(String ua) {
        return ua != null && (isAndroidTablet(ua) || isIpad(ua) || (ua.contains("touch") && ua.contains("win")));
    }

    public static boolean isTablet(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isTablet(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isAndroidMobile(String ua) {
        return ua != null && ua.contains("android") && ua.contains("mobile");
    }

    public static boolean isAndroidMobile(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isAndroidMobile(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isAndroidTablet(String ua) {
        return ua != null && ua.contains("android") && !ua.contains("mobile");
    }

    public static boolean isAndroidTablet(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isAndroidMobile(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isIpad(String ua) {
        return ua != null && ua.contains("ipad");
    }

    public static boolean isIpad(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isIpad(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isIphone(String ua) {
        return ua != null && ua.contains("iphone");
    }

    public static boolean isIphone(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isIpad(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isIeMobile(String ua) {
        return ua != null && ua.contains("iemobile");
    }

    public static boolean isIeMobile(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isIpad(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isWebkit(String ua) {
        return ua != null && ua.contains("applewebkit/");
    }

    public static boolean isWebkit(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isWebkit(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isGecko(String ua) {
        return ua != null && ua.contains("gecko/");
    }

    public static boolean isGecko(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isWebkit(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isTrident(String ua) {
        return ua != null && ua.contains("trident/");
    }

    public static boolean isTrident(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isWebkit(ua.toLowerCase());
    }

    /**
     * @param ua User-agent in lower case
     */
    public static boolean isEdge(String ua) {
        return ua != null && ua.contains("edge/");
    }

    public static boolean isEdge(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        return ua != null && isWebkit(ua.toLowerCase());
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

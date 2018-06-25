package com.softmotions.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.collections4.iterators.IteratorEnumeration;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DetachedHttpServletRequest implements HttpServletRequest {

    private static final SimpleDateFormat[] formatsTemplate = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };

    private final HttpServletRequest req;

    private final boolean allowDelegate;

    private final String method;

    private final String authType;

    private final String contentType;

    private final String pathInfo;

    private final String pathTranslated;

    private final String contextPath;

    private final String queryString;

    private final String remoteUser;

    private final String requestedSessionId;

    private final String requestURI;

    @SuppressWarnings("StringBufferField")
    private final StringBuffer requestURL;

    private final String servletPath;

    private final Principal userPrincipal;

    private final boolean isRequestedSessionIdFromCookie;

    private final boolean isRequestedSessionIdFromURL;

    private final boolean isRequestedSessionIdValid;

    private final boolean isSecure;

    private final String scheme;

    private final String protocol;

    private final Locale locale;

    private final String localAddr;

    private final String localName;

    private final int localPort;

    private final String remoteAddr;

    private final String remoteHost;

    private final int remotePort;

    private final String serverName;

    private final int serverPort;

    private final long contentLengthLong;

    private final Collection<Part> parts;

    private final ServletContext servletContext;

    private final DispatcherType dispatcherType;

    private final Cookie[] cookies;

    private final Map<String, List<String>> headers = new HashMap<>();

    private final Map<String, Object> attributes = new HashMap<>();

    private final Map<String, String[]> parameters;

    private String characterEncoding;


    public DetachedHttpServletRequest(HttpServletRequest req, boolean allowDelegate) {

        this.req = req;
        this.allowDelegate = allowDelegate;
        method = req.getMethod();
        authType = req.getAuthType();
        contentType = req.getContentType();
        contextPath = req.getContextPath();
        cookies = req.getCookies();
        pathInfo = req.getPathInfo();
        pathTranslated = req.getPathTranslated();
        queryString = req.getQueryString();
        remoteUser = req.getRemoteUser();
        requestedSessionId = req.getRequestedSessionId();
        requestURI = req.getRequestURI();
        requestURL = req.getRequestURL();
        servletPath = req.getServletPath();
        userPrincipal = req.getUserPrincipal();
        isRequestedSessionIdFromCookie = req.isRequestedSessionIdFromCookie();
        isRequestedSessionIdFromURL = req.isRequestedSessionIdFromURL();
        isRequestedSessionIdValid = req.isRequestedSessionIdValid();
        isSecure = req.isSecure();
        locale = req.getLocale();
        scheme = req.getScheme();
        protocol = req.getProtocol();
        localAddr = req.getLocalAddr();
        localName = req.getLocalName();
        localPort = req.getLocalPort();
        remoteAddr = req.getRemoteAddr();
        remoteHost = req.getRemoteHost();
        remotePort = req.getRemotePort();
        serverName = req.getServerName();
        serverPort = req.getServerPort();
        characterEncoding = req.getCharacterEncoding();
        contentLengthLong = req.getContentLengthLong();
        servletContext = req.getServletContext();
        dispatcherType = req.getDispatcherType();
        parameters = req.getParameterMap();

        Collection<Part> p = null;
        try {
            p = req.getParts();
        } catch (Throwable ignored) {
        }
        parts = (p != null) ? p : Collections.emptyList();

        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement();
            headers.put(hn, EnumerationUtils.toList(req.getHeaders(hn)));
        }

        Enumeration<String> attributeNames = req.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String an = attributeNames.nextElement();
            attributes.put(an, req.getAttribute(an));
        }
    }

    public HttpServletRequest getWrappedRequest() {
        return req;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getAuthType() {
        return authType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return pathTranslated;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getRemoteUser() {
        return remoteUser;
    }

    @Override
    public String getRequestedSessionId() {
        return requestedSessionId;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public StringBuffer getRequestURL() {
        return requestURL;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public String getHeader(String name) {
        List<String> h = headers.get(name);
        return h.isEmpty() ? null : h.get(0);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> h = headers.get(name);
        return h != null ? new IteratorEnumeration<>(h.iterator()) : Collections.emptyEnumeration();
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return new IteratorEnumeration<>(headers.keySet().iterator());
    }

    @Override
    public int getIntHeader(String name) {
        String h = getHeader(name);
        return h != null ? Integer.parseInt(h) : -1;
    }

    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return (-1L);
        }
        // Attempt to convert the date header in a variety of formats
        Long result = internalParseDate(value, formatsTemplate);
        if (result != null && result != (-1L)) {
            return result;
        }
        throw new IllegalArgumentException(value);
    }

    @Nullable
    private static Long internalParseDate(String value, DateFormat[] formats) {
        Date date = null;
        for (int i = 0; (date == null) && (i < formats.length); i++) {
            try {
                date = formats[i].parse(value);
            } catch (ParseException ignored) {
                // Ignore
            }
        }
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new IteratorEnumeration<>(attributes.keySet().iterator());
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public String getParameter(String name) {
        String[] parameterValues = getParameterValues(name);
        return parameterValues != null && parameterValues.length > 0 ? parameterValues[0] : null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return new IteratorEnumeration<>(parameters.keySet().iterator());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return isRequestedSessionIdFromCookie;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return isRequestedSessionIdFromURL;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return isRequestedSessionIdValid;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public Collection<Part> getParts() {
        return parts;
    }

    @Nullable
    @Override
    public Part getPart(String name) {
        return parts.stream().filter(part -> name.equals(part.getName())).findFirst().orElse(null);
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public String getLocalAddr() {
        return localAddr;
    }

    @Override
    public String getRemoteAddr() {
        return remoteAddr;
    }

    @Override
    public String getRemoteHost() {
        return remoteHost;
    }

    @Override
    public int getRemotePort() {
        return remotePort;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public int getContentLength() {
        return (int) getContentLengthLong();
    }

    @Override
    public long getContentLengthLong() {
        return contentLengthLong;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return dispatcherType;
    }


    ///////////////////////////////////////////////////////////////////////////
    //                                Delegate                               //
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Enumeration<Locale> getLocales() {
        return new IteratorEnumeration<>(Collections.singletonList(getLocale()).iterator());
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        checkDelegate();
        return req.getRequestDispatcher(path);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        checkDelegate();
        return req.getInputStream();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        checkDelegate();
        return req.getReader();
    }

    @Override
    public String getRealPath(String path) {
        checkDelegate();
        //noinspection deprecation
        return req.getRealPath(path);
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        checkDelegate();
        return req.startAsync();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest,
                                   ServletResponse servletResponse) throws IllegalStateException {
        checkDelegate();
        return req.startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        checkDelegate();
        return req.isAsyncStarted();
    }

    @Override
    public boolean isAsyncSupported() {
        checkDelegate();
        return req.isAsyncSupported();
    }

    @Override
    public AsyncContext getAsyncContext() {
        checkDelegate();
        return req.getAsyncContext();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        checkDelegate();
        return req.authenticate(response);
    }

    @Override
    public void login(String username, String password) throws ServletException {
        checkDelegate();
        req.login(username, password);
    }

    @Override
    public void logout() throws ServletException {
        checkDelegate();
        req.logout();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        checkDelegate();
        return req.upgrade(handlerClass);
    }

    @Override
    public boolean isUserInRole(String role) {
        checkDelegate();
        return req.isUserInRole(role);
    }

    @Override
    public HttpSession getSession(boolean create) {
        checkDelegate();
        return req.getSession(create);
    }

    @Override
    public HttpSession getSession() {
        checkDelegate();
        return req.getSession();
    }

    @Override
    public String changeSessionId() {
        checkDelegate();
        return req.changeSessionId();
    }

    private void checkDelegate() {
        if (!allowDelegate) {
            throw new UnsupportedOperationException();
        }
    }
}

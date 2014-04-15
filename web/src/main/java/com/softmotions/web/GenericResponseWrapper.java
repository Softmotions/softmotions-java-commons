package com.softmotions.web;

import org.apache.commons.io.output.WriterOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Provides a wrapper for {@link javax.servlet.http.HttpServletResponseWrapper}.
 * <p/>
 * It is used to wrap the real Response so that we can modify it after
 * that the target of the request has delivered its response.
 * <p/>
 * It uses the Wrapper pattern.
 *
 * @author Adamansky Anton (anton@adamansky.com)
 */
public class GenericResponseWrapper extends HttpServletResponseWrapper implements Serializable {

    private static final long serialVersionUID = -1L;
    private static final Logger log = LoggerFactory.getLogger(GenericResponseWrapper.class);

    private int statusCode = SC_OK;
    private int contentLength;
    private String contentType;

    private Map headerTracker;
    private List headers;
    private List cookies;

    private ServletOutputStream outstr;
    private PrintWriter outwr;
    private boolean delegate;


    public GenericResponseWrapper(HttpServletResponse response, Writer outwr, boolean delegate) {
        super(response);
        this.outwr = new PrintWriter(outwr);
        this.delegate = delegate;
    }

    /**
     * Creates a GenericResponseWrapper
     */
    public GenericResponseWrapper(HttpServletResponse response, OutputStream outstr, boolean delegate) {
        super(response);
        this.outstr = new FilterServletOutputStream(outstr);
        this.delegate = delegate;
    }


    public boolean isDelegate() {
        return delegate;
    }

    /**
     * Gets the outputstream.
     */
    public ServletOutputStream getOutputStream() {
        if (outstr == null) {
            outstr = new FilterServletOutputStream(new WriterOutputStream(outwr, getCharacterEncoding()));
        }
        return outstr;
    }

    /**
     * Gets the print writer.
     */
    public PrintWriter getWriter() throws IOException {
        if (outwr == null) {
            outwr = new PrintWriter(new OutputStreamWriter(outstr, getCharacterEncoding()));
        }
        return outwr;
    }

    /**
     * Sets the status code for this response.
     */
    public void setStatus(final int code) {
        statusCode = code;
        if (delegate) {
            super.setStatus(code);
        }
    }

    /**
     * Send the error. If the response is not ok, most of the logic is bypassed and the error is sent raw
     * Also, the content is not cached.
     *
     * @param i      the status code
     * @param string the error message
     * @throws IOException
     */
    public void sendError(int i, String string) throws IOException {
        statusCode = i;
        if (delegate) {
            super.sendError(i, string);
        }
    }

    /**
     * Send the error. If the response is not ok, most of the logic is bypassed and the error is sent raw
     * Also, the content is not cached.
     *
     * @param i the status code
     * @throws IOException
     */
    public void sendError(int i) throws IOException {
        statusCode = i;
        if (delegate) {
            super.sendError(i);
        }
    }

    /**
     * Send the redirect. If the response is not ok, most of the logic is bypassed and the error is sent raw.
     * Also, the content is not cached.
     *
     * @param string the URL to redirect to
     * @throws IOException
     */
    public void sendRedirect(String string) throws IOException {
        statusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
        if (delegate) {
            super.sendRedirect(string);
        }
    }

    /**
     * Sets the status code for this response.
     */
    public void setStatus(final int code, final String msg) {
        statusCode = code;
        log.warn("Discarding message because this method is deprecated.");
        if (delegate) {
            super.setStatus(code);
        }
    }

    /**
     * Returns the status code for this response.
     */
    public int getStatus() {
        return statusCode;
    }

    /**
     * Sets the content length.
     */
    public void setContentLength(final int length) {
        this.contentLength = length;
        if (delegate) {
            super.setContentLength(length);
        }
    }

    /**
     * Gets the content length.
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * Sets the content type.
     */
    public void setContentType(final String type) {
        this.contentType = type;
        if (delegate) {
            super.setContentType(type);
        }
    }

    /**
     * Gets the content type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Adds a header, even if one already exists, in accordance with the spec
     */
    public void addHeader(final String name, final String value) {
        final String[] header = new String[]{name, value};
        if (headers == null) {
            headers = new ArrayList();
        }
        headers.add(header);
        if (delegate) {
            super.addHeader(name, value);
        }
        if (headerTracker == null) {
            headerTracker = new HashMap();
        }
        Integer count = (Integer) headerTracker.get(name.toLowerCase());
        if (count == null) {
            count = new Integer(1);
        } else {
            count = new Integer(count.intValue() + 1);
        }
        headerTracker.put(name.toLowerCase(), count);
    }

    /**
     * Sets a header overwriting any previous values for the header if
     * it existed.
     */
    public void setHeader(final String name, final String value) {
        if (delegate) {
            super.setHeader(name, value);
        }
        if (headerTracker == null) {
            headerTracker = new HashMap();
        }
        if (headers == null) {
            headers = new ArrayList();
        }
        Integer count = (Integer) headerTracker.get(name);
        if (count != null && count.intValue() > 0) {
            for (int i = headers.size() - 1; i >= 0; i--) {
                String[] header = (String[]) headers.get(i);
                String hName = header[0];
                if (hName.equalsIgnoreCase(name)) {
                    if (count > 1) {
                        headers.remove(i);
                        count = count.intValue() - 1;
                        headerTracker.put(name.toLowerCase(), count);
                    } else {
                        ((String[]) headers.get(i))[1] = value;
                    }

                }
            }
        } else {
            headerTracker.put(name.toLowerCase(), value);
        }
    }

    /**
     * Gets the headers.
     */
    public Collection getHeaders() {
        return headers != null ? headers : Collections.EMPTY_LIST;
    }

    /**
     * Adds a cookie.
     */
    public void addCookie(final Cookie cookie) {
        if (cookies == null) {
            cookies = new ArrayList();
        }
        cookies.add(cookie);
        if (delegate) {
            super.addCookie(cookie);
        }
    }

    /**
     * Gets all the cookies.
     */
    public Collection getCookies() {
        return cookies != null ? cookies : Collections.EMPTY_LIST;
    }

    /**
     * Flushes buffer and commits response to client.
     */
    public void flushBuffer() throws IOException {
        flush();
        if (delegate) {
            super.flushBuffer();
        }
    }

    /**
     * Resets the response.
     */
    public void reset() {
        if (delegate) {
            super.reset();
        }
        if (cookies != null) {
            cookies.clear();
        }
        if (headers != null) {
            headers.clear();
        }
        statusCode = SC_OK;
        contentType = null;
        contentLength = 0;
    }

    /**
     * Resets the buffers.
     */
    public void resetBuffer() {
        if (delegate) {
            super.resetBuffer();
        }
    }

    /**
     * Flushes all the streams for this response.
     */
    public void flush() throws IOException {
        if (outwr != null) {
            outwr.flush();
        } else {
            outstr.flush();
        }
    }


    public static class FilterServletOutputStream extends ServletOutputStream {

        private final OutputStream stream;

        /**
         * Creates a FilterServletOutputStream.
         */
        public FilterServletOutputStream(final OutputStream stream) {
            this.stream = stream;
        }

        /**
         * Writes to the stream.
         */
        public void write(final int b) throws IOException {
            stream.write(b);
        }

        /**
         * Writes to the stream.
         */
        public void write(final byte[] b) throws IOException {
            stream.write(b);
        }

        /**
         * Writes to the stream.
         */
        public void write(final byte[] b, final int off, final int len) throws IOException {
            stream.write(b, off, len);
        }

        public void flush() throws IOException {
            stream.flush();
        }

        public boolean isReady() {
            return true;
        }

        public void setWriteListener(WriteListener writeListener) {
            try {
                writeListener.onWritePossible();
            } catch (IOException e) {
                log.error("", e);
            }
        }
    }
}

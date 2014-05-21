package com.softmotions.commons.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class InputStreamWrapper extends InputStream {

    private final InputStream is;

    public InputStreamWrapper(InputStream is) {
        this.is = is;
    }

    protected InputStream getWrapped() {
        return is;
    }

    public int read() throws IOException {
        return is.read();
    }

    public int read(byte[] b) throws IOException {
        return is.read(b);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        return is.read(b, off, len);
    }

    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    public int available() throws IOException {
        return is.available();
    }

    public void close() throws IOException {
        is.close();
    }

    public synchronized void mark(int readlimit) {
        is.mark(readlimit);
    }

    public synchronized void reset() throws IOException {
        is.reset();
    }

    public boolean markSupported() {
        return is.markSupported();
    }
}

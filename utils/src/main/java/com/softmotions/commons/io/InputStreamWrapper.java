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

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return is.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return is.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return is.skip(n);
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public void close() throws IOException {
        is.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        is.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        is.reset();
    }

    @Override
    public boolean markSupported() {
        return is.markSupported();
    }
}

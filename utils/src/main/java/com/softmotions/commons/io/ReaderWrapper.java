package com.softmotions.commons.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class ReaderWrapper extends Reader {

    private final Reader r;

    public ReaderWrapper(Reader r) {
        this.r = r;
    }

    protected Reader getWrapped() {
        return r;
    }

    public int read(CharBuffer target) throws IOException {
        return r.read(target);
    }

    public int read() throws IOException {
        return r.read();
    }

    public int read(char[] cbuf) throws IOException {
        return r.read(cbuf);
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        return r.read(cbuf, off, len);
    }

    public long skip(long n) throws IOException {
        return r.skip(n);
    }

    public boolean ready() throws IOException {
        return r.ready();
    }

    public boolean markSupported() {
        return r.markSupported();
    }

    public void mark(int readAheadLimit) throws IOException {
        r.mark(readAheadLimit);
    }

    public void reset() throws IOException {
        r.reset();
    }

    public void close() throws IOException {
        r.close();
    }

}

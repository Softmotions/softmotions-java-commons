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

    @Override
    public int read(CharBuffer target) throws IOException {
        return r.read(target);
    }

    @Override
    public int read() throws IOException {
        return r.read();
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        return r.read(cbuf);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        return r.read(cbuf, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return r.skip(n);
    }

    @Override
    public boolean ready() throws IOException {
        return r.ready();
    }

    @Override
    public boolean markSupported() {
        return r.markSupported();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        r.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        r.reset();
    }

    @Override
    public void close() throws IOException {
        r.close();
    }

}

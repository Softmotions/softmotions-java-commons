package com.softmotions.commons.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class LimitedInputStream extends FilterInputStream {

    private long count;
    private long mark = -1L;
    private final long limit;
    private final String exceptionMessage;

    public LimitedInputStream(long limit, String exceptionMessage, InputStream in) {
        super(in);
        this.limit = limit;
        this.exceptionMessage = exceptionMessage;
    }

    public long getCount() {
        return this.count;
    }

    @Override
    public int read() throws IOException {
        int result = this.in.read();
        if (result != -1) {
            checkLimits(1);
            ++this.count;
        }

        return result;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int result = this.in.read(b, off, len);
        if (result != -1) {
            checkLimits(result);
            this.count += (long) result;
        }

        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        long result = this.in.skip(n);
        checkLimits(result);
        this.count += result;
        return result;
    }

    @Override
    public synchronized void mark(int readlimit) {
        this.in.mark(readlimit);
        this.mark = this.count;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (!this.in.markSupported()) {
            throw new IOException("Mark not supported");
        } else if (this.mark == -1L) {
            throw new IOException("Mark not set");
        } else {
            this.in.reset();
            this.count = this.mark;
        }
    }

    private void checkLimits(long add) throws LimitationIOException {
        if (count + add > limit) {
            throw new LimitationIOException(limit, exceptionMessage);
        }
    }
}

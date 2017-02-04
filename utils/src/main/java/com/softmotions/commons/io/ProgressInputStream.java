package com.softmotions.commons.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} that can report the progress of reading from another
 * InputStream.
 */
public class ProgressInputStream extends InputStream {

    private final InputStream stream;
    private final ProgressListener listener;

    private long total;
    private long totalRead;

    public ProgressInputStream(InputStream stream, ProgressListener listener, long total) {
        this.stream = stream;
        this.listener = listener;
        this.total = total;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }


    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    @Override
    public int read() throws IOException {
        int read = this.stream.read();
        this.totalRead++;
        this.listener.onProgressChanged(this.totalRead, this.total);
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = this.stream.read(b, off, len);
        this.totalRead += read;
        this.listener.onProgressChanged(this.totalRead, this.total);
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        return stream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return stream.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        stream.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }

    @Override
    public synchronized void reset() throws IOException {
        stream.reset();
    }
}
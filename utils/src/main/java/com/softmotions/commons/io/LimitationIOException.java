package com.softmotions.commons.io;

import java.io.IOException;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class LimitationIOException extends IOException {

    long limit;

    public long getLimit() {
        return limit;
    }

    public LimitationIOException(long limit, String msg) {
        super(msg);
        this.limit = limit;

    }
}

package com.softmotions.commons.io;

public interface ProgressListener {

    /**
     * IO progress listener.
     *
     * @param numBytes   the number of bytes completed.
     * @param totalBytes the total number of bytes or -1 if unknown.
     */
    void onProgressChanged(long numBytes, long totalBytes);
}
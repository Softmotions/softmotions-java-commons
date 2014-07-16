package com.softmotions.commons.io.scanner;

import java.io.File;
import java.io.IOException;

/**
 * Abstract directory scanner.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DirectoryScanner {

    protected File basedir;

    public DirectoryScanner setBaseDir(File basedir) throws IOException {
        if (!basedir.isDirectory()) {
            throw new IOException("Not a directory: " + basedir);
        }
        this.basedir = basedir;
        return this;
    }
}

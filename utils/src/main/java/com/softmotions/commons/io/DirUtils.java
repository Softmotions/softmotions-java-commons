package com.softmotions.commons.io;

import java.io.File;
import java.io.IOException;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class DirUtils {

    private DirUtils() {
    }

    public static void ensureDir(File dir, boolean canWrite) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Cannot create directory: " + dir.getAbsolutePath());
            }
        }
        if (canWrite && !dir.canWrite()) {
            throw new IOException("Directory: " + dir.getAbsolutePath() + " is not writable");
        }
    }
}

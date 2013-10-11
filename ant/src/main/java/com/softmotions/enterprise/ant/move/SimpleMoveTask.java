package com.softmotions.enterprise.ant.move;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.IOException;

/**
 * Очень простая и быстрая таска
 * для перемещения или переименования файлов
 * <p/>
 * Зависит от apache commons-io
 *
 * @author Adamansky Anton (anton@adamansky.com)
 */
public class SimpleMoveTask extends Task {

    private File file;

    private File tofile;


    public void setFile(File file) {
        this.file = file;
    }

    public void setTofile(File tofile) {
        this.tofile = tofile;
    }

    public String getTaskName() {
        return "simpleMove";
    }

    public void execute() throws BuildException {
        if (file == null) {
            throw new BuildException("Missing required attribute 'file'");
        }
        if (tofile == null) {
            throw new BuildException("Missing required attribute 'tofile'");
        }
        log("Moving: " + file.getPath() + " to: " + tofile.getPath(), Project.MSG_INFO);
        if (!file.exists()) {
            throw new BuildException("File: " + file + " does not exists");
        }
        if (tofile.exists()) {
            if (tofile.isFile() && !tofile.delete()) {
                throw new BuildException("Cannot delete 'tofile': " + tofile);
            } else if (tofile.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(tofile);
                } catch (IOException e) {
                    throw new BuildException(e);
                }
            }
        }
        File pto = tofile.getParentFile();
        if (pto != null && !pto.exists()) {
            if (!pto.mkdirs()) {
                throw new BuildException("Cannot make dirs: " + pto);
            }
        }
        if (!file.renameTo(tofile)) {
            throw new BuildException("Cannot rename: " + file + " to: " + tofile);
        }
    }
}

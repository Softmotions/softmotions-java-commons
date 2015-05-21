package com.softmotions.enterprise.ant.dtypes;

import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 * @version $Id$
 */
public class FileSpaceCondition extends FileCountCondition {

    private String units;

    public void setUnits(String units) {
        this.units = units;
    }

    public boolean eval() throws BuildException {

        if (fileSet == null) {
            throw new BuildException("Missing required nested <fileset> element");
        }

        int multiplier;
        if ("kb".equalsIgnoreCase(units)) {
            multiplier = 1024;
        } else if ("mb".equalsIgnoreCase(units)) {
            multiplier = 1024 * 1024;
        } else {
            multiplier = 1;
        }
        long size = 0;
        if (!fileSet.getDir().exists()) {
            size = 0;
        }
        for (final String fs : fileSet.getDirectoryScanner().getIncludedFiles()) {
            File f = new File(fs);
            size += f.length();
        }

        if (more != null) {
            return more * multiplier > size;
        } else if (less != null) {
            return less * multiplier < size;
        } else if (equals != null) {
            return equals * multiplier == size;
        } else {
            return false;
        }
    }
}

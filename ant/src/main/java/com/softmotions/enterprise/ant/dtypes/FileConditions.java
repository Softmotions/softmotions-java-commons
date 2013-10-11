/*
 * Copyright (c) 2010 Softmotions
 * All Rights Reserved
 *
 * $Id: FileConditions.java 14054 2010-06-16 07:31:09Z adam $
 */
package com.softmotions.enterprise.ant.dtypes;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.condition.Condition;

import java.io.File;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 */
public class FileConditions implements Condition {

    private File file;

    private String condition;

    public void setFile(File file) {
        this.file = file;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public boolean eval() throws BuildException {
        boolean res = false;
        if (file == null) {
            throw new BuildException("Missing required  'file' attribute");
        }
        //System.out.println("file=" + file.getAbsolutePath());
        if ("exists".equals(condition)) {
            res = file.exists();
        } else if ("dir-exists".equals(condition)) {
            res = file.exists() && file.isDirectory();
        } else if ("can-read".equals(condition)) {
            res = file.canRead();
        } else if ("can-write".equals(condition)) {
            res = file.canWrite();
        } else if ("can-execute".equals(condition)) {
            res = file.canExecute();
        } else {
            throw new BuildException("Invalid 'condition' attribute value: '" + condition + '\'');
        }
        return res;
    }
}

package com.softmotions.enterprise.ant.dtypes;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.FileSet;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 * @version $Id$
 */
public class FileCountCondition extends DataType implements Condition {

    protected Integer more;

    protected Integer less;

    protected Integer equals;

    protected FileSet fileSet;

    public void setMore(Integer more) {
        this.more = more;
    }

    public void setLess(Integer less) {
        this.less = less;
    }

    public void setEquals(Integer equals) {
        this.equals = equals;
    }

    public void addFileSet(FileSet fileSet) {
        this.fileSet = fileSet;
    }

    @Override
    public boolean eval() throws BuildException {
        if (fileSet == null) {
            throw new BuildException("Missing required nested <fileset> element");
        }
        int fcount = 0;
        if (fileSet.getDir().exists()) {
            fcount = fileSet.getDirectoryScanner().getIncludedFilesCount();
            log("FCOUNT=" + fcount + ", FILES=" + fileSet, Project.MSG_INFO);
        }
        boolean res;
        if (more != null && fcount > more.intValue()) {
            res = true;
        } else if (less != null && fcount < less.intValue()) {
            res = true;
        } else if (equals != null && fcount == equals.intValue()) {
            res = true;
        } else {
            res = false;
        }
        log("FILE_COUNT_CONDITION RESULT: " + res, Project.MSG_INFO);
        return res;
    }

}

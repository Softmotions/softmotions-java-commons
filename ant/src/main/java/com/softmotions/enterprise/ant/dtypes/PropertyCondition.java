package com.softmotions.enterprise.ant.dtypes;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.apache.tools.ant.taskdefs.condition.ConditionBase;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 * @version $Id$
 */
public class PropertyCondition extends ConditionBase implements Condition {

    private String property;

    private boolean invert;

    private String value;


    public void setProperty(String property) {
        this.property = property;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean eval() throws BuildException {
        if (property == null) {
            throw new BuildException("Missing required attribute 'property'");
        }
        String p = getProject().getProperty(property);
        boolean res;
        if (value != null) {
            res = !invert ? value.equals(p) : !value.equals(p);
        } else {
            res = !invert ? p != null : p == null;
        }
        log("PropertyCondition property=" + property + " pval=" + p + " val=" + value + " res=" + res, Project.MSG_INFO);
        return res;
    }
}

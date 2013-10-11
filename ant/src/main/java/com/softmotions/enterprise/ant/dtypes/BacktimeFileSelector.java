package com.softmotions.enterprise.ant.dtypes;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.selectors.BaseSelector;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class BacktimeFileSelector extends BaseSelector {


    private String pattern;

    private String date;

    private Integer deltaseconds;


    private long checkDate;


    public void setPattern(String pattern) {
        this.pattern = pattern;
        tryInit();
    }

    public void setDate(String date) {
        this.date = date;
        tryInit();
    }

    public void setDeltaseconds(Integer deltaseconds) {
        this.deltaseconds = deltaseconds;
        tryInit();
    }


    private void tryInit() throws BuildException {
        if (pattern != null && date != null && deltaseconds != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            Date cdate;
            try {
                cdate = sdf.parse(date);
            } catch (ParseException e) {
                throw new BuildException(e);
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(cdate);
            cal.add(Calendar.SECOND, deltaseconds);
            checkDate = cal.getTimeInMillis();
        }
    }

    public boolean isSelected(File basedir, String filename, File file) throws BuildException {
        if (pattern == null) {
            throw new BuildException("Missing required attribute: 'pattern'");
        }
        if (date == null) {
            throw new BuildException("Missing required attribute: 'date'");
        }
        if (deltaseconds == null) {
            throw new BuildException("Missing required attribute: 'deltaseconds'");
        }

        return (file.lastModified() <= checkDate);
    }
}

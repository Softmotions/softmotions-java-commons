package com.softmotions.enterprise.ant.misc;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.awt.*;
import java.net.URI;

/**
 * Opens the given url in the browser
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class OpenURLInBrowserTask extends Task {

    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public void execute() throws BuildException {
        if (!Desktop.isDesktopSupported()) {
            log("Desktop is not supported", Project.MSG_WARN);
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            log("Desktop doesn't support the browse action ", Project.MSG_WARN);
            return;
        }
        try {
            URI uri = new URI(url);
            desktop.browse(uri);
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }
}

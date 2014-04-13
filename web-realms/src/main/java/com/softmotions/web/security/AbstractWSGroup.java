package com.softmotions.web.security;

import javax.security.auth.Subject;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class AbstractWSGroup implements WSGroup {

    /**
     * The description of this group.
     */
    protected String description;

    /**
     * The group name of this group.
     */
    protected String groupname;

    protected AbstractWSGroup() {
    }

    protected AbstractWSGroup(String groupname, String description) {
        this.groupname = groupname;
        this.description = description;
    }

    public String getName() {
        return getGroupname();
    }

    public boolean implies(Subject subject) {
        if (subject == null)
            return false;
        return subject.getPrincipals().contains(this);
    }

    public String getGroupname() {
        return groupname;
    }

    public void setGroupname(String groupname) {
        this.groupname = groupname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

package com.softmotions.web.security;

import javax.security.auth.Subject;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class AbstractWSRole implements WSRole {

    /**
     * The description of this Role.
     */
    protected String description;

    /**
     * The role name of this Role.
     */
    protected String rolename;

    public String getRolename() {
        return rolename;
    }

    public void setRolename(String rolename) {
        this.rolename = rolename;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return getRolename();
    }

    public boolean implies(Subject subject) {
        return (subject != null && subject.getPrincipals().contains(this));
    }

    protected AbstractWSRole(String rolename, String description) {
        this.rolename = rolename;
        this.description = description;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractWSRole that = (AbstractWSRole) o;
        return rolename.equals(that.rolename);
    }

    public int hashCode() {
        return rolename.hashCode();
    }
}

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
    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public boolean implies(Subject subject) {
        return (subject != null && subject.getPrincipals().contains(this));
    }

    protected AbstractWSRole(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractWSRole that = (AbstractWSRole) o;
        return name.equals(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }
}

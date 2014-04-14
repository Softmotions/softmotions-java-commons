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
    protected String name;

    protected AbstractWSGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean implies(Subject subject) {
        return (subject != null && subject.getPrincipals().contains(this));
    }

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

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractWSGroup that = (AbstractWSGroup) o;
        return name.equals(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
        sb.append("name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

package com.softmotions.web.security;

import javax.security.auth.Subject;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class AbstractWSUser implements WSUser {

    /**
     * The full name of this user.
     */
    protected String fullName;

    /**
     * The logon password of this user.
     */
    protected String password;

    /**
     * The logon username of this user.
     */
    protected String name;


    public boolean implies(Subject subject) {
        return (subject != null && subject.getPrincipals().contains(this));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    protected AbstractWSUser(String name, String fullName) {
        this.name = name;
        this.fullName = fullName;
    }

    protected AbstractWSUser(String name, String fullName, String password) {
        this.name = name;
        this.fullName = fullName;
        this.password = password;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractWSUser that = (AbstractWSUser) o;
        return name.equals(that.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
        sb.append("name='").append(name).append('\'');
        sb.append(", fullName='").append(fullName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

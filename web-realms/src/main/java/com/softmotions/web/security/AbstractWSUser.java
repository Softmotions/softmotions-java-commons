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
    protected String username;


    public String getName() {
        return getUsername();
    }

    public boolean implies(Subject subject) {
        if (subject == null)
            return false;
        return subject.getPrincipals().contains(this);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    protected AbstractWSUser() {
    }

    protected AbstractWSUser(String username, String fullName) {
        this.username = username;
        this.fullName = fullName;
    }

    protected AbstractWSUser(String username, String fullName, String password) {
        this.username = username;
        this.fullName = fullName;
        this.password = password;
    }


}

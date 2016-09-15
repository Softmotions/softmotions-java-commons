package com.softmotions.web.security;

import javax.security.auth.Subject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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


    protected String email;


    @Override
    public boolean implies(Subject subject) {
        return (subject != null && subject.getPrincipals().contains(this));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean matchPassword(String inputPassword) throws NoSuchAlgorithmException {
        return inputPassword.equals(password);
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String[] getRoleNames() {
        List<String> rnames = new ArrayList<>();
        Iterator<WSRole> roles = getRoles();
        while (roles.hasNext()) {
            rnames.add(roles.next().getName());
        }
        return rnames.toArray(new String[rnames.size()]);
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

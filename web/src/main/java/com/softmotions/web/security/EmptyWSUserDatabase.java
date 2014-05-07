package com.softmotions.web.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class EmptyWSUserDatabase implements WSUserDatabase {

    protected final String databaseName;

    public EmptyWSUserDatabase(String name) {
        this.databaseName = name;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public Iterator<WSGroup> getGroups() {
        return Collections.emptyIterator();
    }

    public Iterator<WSRole> getRoles() {
        return Collections.emptyIterator();
    }

    public Iterator<WSUser> getUsers() {
        return Collections.emptyIterator();
    }

    public int getUsersCount() {
        return 0;
    }

    public int getUsersCount(String query) {
        return 0;
    }

    public Iterator<WSUser> getUsers(String query, String orderProperty, boolean desc, int skip, int limit) {
        return Collections.emptyIterator();
    }

    public WSGroup findGroup(String groupname) {
        return null;
    }

    public WSRole findRole(String rolename) {
        return null;
    }

    public WSUser findUser(String username) {
        return null;
    }

    public WSGroup createGroup(String groupname, String description) {
        throw new UnsupportedOperationException("createGroup");
    }

    public WSRole createRole(String rolename, String description) {
        throw new UnsupportedOperationException("createRole");
    }

    public WSUser createUser(String username, String password, String fullName) {
        throw new UnsupportedOperationException("createUser");
    }

    public void removeGroup(WSGroup group) {
        throw new UnsupportedOperationException("createRole");
    }

    public void removeRole(WSRole role) {
        throw new UnsupportedOperationException("removeRole");
    }

    public void removeUser(WSUser user) {
        throw new UnsupportedOperationException("removeUser");
    }

    public void close() throws IOException {
    }
}

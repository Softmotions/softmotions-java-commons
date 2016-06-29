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

    @Override
    public boolean isCanUsersWrite() {
        return false;
    }

    @Override
    public boolean isCanUsersAccessWrite() {
        return false;
    }

    @Override
    public boolean isCanGroupsWrite() {
        return false;
    }

    @Override
    public boolean isCanRolesWrite() {
        return false;
    }

    @Override
    public int getWriteMask() {
        return 0;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public Iterator<WSGroup> getGroups() {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<WSRole> getRoles() {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<WSUser> getUsers() {
        return Collections.emptyIterator();
    }

    @Override
    public int getUsersCount() {
        return 0;
    }

    @Override
    public int getUsersCount(String query) {
        return 0;
    }

    @Override
    public int getActiveUsersCount(String query) {
        return 0;
    }

    @Override
    public Iterator<WSUser> getUsers(String query, String orderProperty, boolean desc, int skip, int limit) {
        return Collections.emptyIterator();
    }

    @Override
    public Iterator<WSUser> getActiveUsers(String query, String orderProperty, boolean desc, int skip, int limit) {
        return Collections.emptyIterator();
    }

    @Override
    public WSGroup findGroup(String groupname) {
        return null;
    }

    @Override
    public WSRole findRole(String rolename) {
        return null;
    }

    @Override
    public WSUser findUser(String username) {
        return null;
    }

    @Override
    public WSGroup createGroup(String groupname, String description) {
        throw new UnsupportedOperationException("createGroup");
    }

    @Override
    public WSRole createRole(String rolename, String description) {
        throw new UnsupportedOperationException("createRole");
    }

    @Override
    public WSUser createUser(String username, String password, String fullName) {
        throw new UnsupportedOperationException("createUser");
    }

    @Override
    public void removeGroup(WSGroup group) {
        throw new UnsupportedOperationException("createRole");
    }

    @Override
    public void removeRole(WSRole role) {
        throw new UnsupportedOperationException("removeRole");
    }

    @Override
    public void removeUser(WSUser user) {
        throw new UnsupportedOperationException("removeUser");
    }

    @Override
    public void close() throws IOException {
    }
}

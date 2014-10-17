package com.softmotions.web.security;

import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
@SuppressWarnings("AbstractClassExtendsConcreteClass")
public abstract class AbstractWSUserDatabaseJNDIFactory extends Reference implements ObjectFactory {

    @SuppressWarnings("StaticCollection")
    protected static final Map<Name, WSUserDatabaseWrapper> DB_CACHE = new HashMap<>();

    public AbstractWSUserDatabaseJNDIFactory(String className, String factory, String factoryLocation) {
        super(className, factory, factoryLocation);
    }

    protected WSUserDatabase createWrapper(WSUserDatabase wsUserDatabase) {
        return new WSUserDatabaseWrapper(wsUserDatabase);
    }

    protected static class WSUserDatabaseWrapper implements WSUserDatabase {

        private final WSUserDatabase wrapped;

        protected WSUserDatabaseWrapper(WSUserDatabase wrapped) {
            this.wrapped = wrapped;
        }

        public boolean isCanUsersWrite() {
            return wrapped.isCanUsersWrite();
        }

        public boolean isCanUsersAccessWrite() {
            return wrapped.isCanUsersAccessWrite();
        }

        public boolean isCanGroupsWrite() {
            return wrapped.isCanGroupsWrite();
        }

        public boolean isCanRolesWrite() {
            return wrapped.isCanRolesWrite();
        }

        public int getWriteMask() {
            return wrapped.getWriteMask();
        }

        public String getDatabaseName() {
            return wrapped.getDatabaseName();
        }

        public Iterator<WSGroup> getGroups() {
            return wrapped.getGroups();
        }

        public Iterator<WSRole> getRoles() {
            return wrapped.getRoles();
        }

        public Iterator<WSUser> getUsers() {
            return wrapped.getUsers();
        }

        public int getUsersCount() {
            return wrapped.getUsersCount();
        }

        public int getUsersCount(String query) {
            return wrapped.getUsersCount(query);
        }

        public int getActiveUsersCount(String query) {
            return wrapped.getActiveUsersCount(query);
        }

        public Iterator<WSUser> getUsers(String query, String orderProperty, boolean desc, int skip, int limit) {
            return wrapped.getUsers(query, orderProperty, desc, skip, limit);
        }

        public Iterator<WSUser> getActiveUsers(String query, String orderProperty, boolean desc, int skip, int limit) {
            return wrapped.getActiveUsers(query, orderProperty, desc, skip, limit);
        }

        public WSGroup createGroup(String s, String s2) {
            return wrapped.createGroup(s, s2);
        }

        public WSRole createRole(String s, String s2) {
            return wrapped.createRole(s, s2);
        }

        public WSUser createUser(String s, String s2, String s3) {
            return wrapped.createUser(s, s2, s3);
        }

        public WSGroup findGroup(String s) {
            return wrapped.findGroup(s);
        }

        public WSRole findRole(String s) {
            return wrapped.findRole(s);
        }

        public WSUser findUser(String s) {
            return wrapped.findUser(s);
        }

        public void removeGroup(WSGroup wsGroup) {
            wrapped.removeGroup(wsGroup);
        }

        public void removeRole(WSRole wsRole) {
            wrapped.removeRole(wsRole);
        }

        public void removeUser(WSUser wsUser) {
            wrapped.removeUser(wsUser);
        }

        public int hashCode() {
            return wrapped.hashCode();
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            return wrapped.equals(obj);
        }

        public void close() throws IOException {
            synchronized (DB_CACHE) {
                List<Name> names = new ArrayList<>();
                for (final Map.Entry<Name, AbstractWSUserDatabaseJNDIFactory.WSUserDatabaseWrapper> e : DB_CACHE.entrySet()) {
                    if (e.getValue().equals(this)) {
                        names.add(e.getKey());
                    }
                }
                for (final Name n : names) {
                    names.remove(n);
                }
            }
        }
    }
}

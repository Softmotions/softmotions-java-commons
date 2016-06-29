package com.softmotions.web.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 * @author Tyutyunkov Vyacheslav (tve@softmotions.com)
 * @version $Id$
 */
@SuppressWarnings("AbstractClassExtendsConcreteClass")
public abstract class AbstractWSUserDatabaseJNDIFactory extends Reference implements ObjectFactory {

    @SuppressWarnings("StaticCollection")
    protected static final Map<Name, WSUserDatabaseWrapper> DB_CACHE = new HashMap<>();

    protected AbstractWSUserDatabaseJNDIFactory(String className, String factory, String factoryLocation) {
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

        @Override
        public boolean isCanUsersWrite() {
            return wrapped.isCanUsersWrite();
        }

        @Override
        public boolean isCanUsersAccessWrite() {
            return wrapped.isCanUsersAccessWrite();
        }

        @Override
        public boolean isCanGroupsWrite() {
            return wrapped.isCanGroupsWrite();
        }

        @Override
        public boolean isCanRolesWrite() {
            return wrapped.isCanRolesWrite();
        }

        @Override
        public int getWriteMask() {
            return wrapped.getWriteMask();
        }

        @Override
        public String getDatabaseName() {
            return wrapped.getDatabaseName();
        }

        @Override
        public Iterator<WSGroup> getGroups() {
            return wrapped.getGroups();
        }

        @Override
        public Iterator<WSRole> getRoles() {
            return wrapped.getRoles();
        }

        @Override
        public Iterator<WSUser> getUsers() {
            return wrapped.getUsers();
        }

        @Override
        public int getUsersCount() {
            return wrapped.getUsersCount();
        }

        @Override
        public int getUsersCount(String query) {
            return wrapped.getUsersCount(query);
        }

        @Override
        public int getActiveUsersCount(String query) {
            return wrapped.getActiveUsersCount(query);
        }

        @Override
        public Iterator<WSUser> getUsers(String query, String orderProperty, boolean desc, int skip, int limit) {
            return wrapped.getUsers(query, orderProperty, desc, skip, limit);
        }

        @Override
        public Iterator<WSUser> getActiveUsers(String query, String orderProperty, boolean desc, int skip, int limit) {
            return wrapped.getActiveUsers(query, orderProperty, desc, skip, limit);
        }

        @Override
        public WSGroup createGroup(String s, String s2) {
            return wrapped.createGroup(s, s2);
        }

        @Override
        public WSRole createRole(String s, String s2) {
            return wrapped.createRole(s, s2);
        }

        @Override
        public WSUser createUser(String s, String s2, String s3) {
            return wrapped.createUser(s, s2, s3);
        }

        @Override
        public WSGroup findGroup(String s) {
            return wrapped.findGroup(s);
        }

        @Override
        public WSRole findRole(String s) {
            return wrapped.findRole(s);
        }

        @Override
        public WSUser findUser(String s) {
            return wrapped.findUser(s);
        }

        @Override
        public void removeGroup(WSGroup wsGroup) {
            wrapped.removeGroup(wsGroup);
        }

        @Override
        public void removeRole(WSRole wsRole) {
            wrapped.removeRole(wsRole);
        }

        @Override
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

        @Override
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

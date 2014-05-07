package com.softmotions.web.security;

import java.io.Closeable;
import java.util.Iterator;

/**
 * [
 * Based on catalina {@link org.apache.catalina.UserDatabase}
 *
 * @author Craig R. McClanahan
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WSUserDatabase extends Closeable {

    /**
     * Return name of user database
     */
    String getDatabaseName();

    /**
     * Return the set of {@link org.apache.catalina.Group}s defined in this user database.
     */
    Iterator<WSGroup> getGroups();

    /**
     * Return the set of {@link org.apache.catalina.Role}s defined in this user database.
     */
    Iterator<WSRole> getRoles();

    /**
     * Return the set of {@link org.apache.catalina.User}s defined in this user database.
     */
    Iterator<WSUser> getUsers();

    /**
     * Return number of users stored in database.
     */
    int getUsersCount();

    /**
     * Return number of records stored in this user database.
     *
     * @param query Optional query text
     * @return
     */
    int getUsersCount(String query);

    /**
     * Return the set of {@link org.apache.catalina.User}s defined in this user database.
     *
     * @param query      Optional query text
     * @param orderField Optional property name used to sort results.
     * @param desc       If true use DESC sorting based on specified orderField otherwise ASC sorting will be used
     * @param skip       Number of records to skip
     * @param limit      Limit number of resulting records
     * @return
     */
    Iterator<WSUser> getUsers(String query, String orderField, boolean desc, int skip, int limit);

    /**
     * Create and return a new {@link WSGroup} defined in this user database.
     *
     * @param groupname   The group name of the new group (must be unique)
     * @param description The description of this group
     */
    WSGroup createGroup(String groupname, String description);

    /**
     * Create and return a new {@link WSRole} defined in this user database.
     *
     * @param rolename    The role name of the new role (must be unique)
     * @param description The description of this role
     */
    WSRole createRole(String rolename, String description);

    /**
     * Create and return a new {@link WSUser} defined in this user database.
     *
     * @param username The logon username of the new user (must be unique)
     * @param password The logon password of the new user
     * @param fullName The full name of the new user
     */
    WSUser createUser(String username, String password,
                      String fullName);

    /**
     * Return the {@link WSGroup} with the specified group name, if any;
     * otherwise return <code>null</code>.
     *
     * @param groupname Name of the group to return
     */
    WSGroup findGroup(String groupname);

    /**
     * Return the {@link WSRole} with the specified role name, if any;
     * otherwise return <code>null</code>.
     *
     * @param rolename Name of the role to return
     */
    WSRole findRole(String rolename);

    /**
     * Return the {@link WSUser} with the specified user name, if any;
     * otherwise return <code>null</code>.
     *
     * @param username Name of the user to return
     */
    WSUser findUser(String username);

    /**
     * Remove the specified {@link WSGroup} from this user database.
     *
     * @param group The group to be removed
     */
    void removeGroup(WSGroup group);

    /**
     * Remove the specified {@link WSRole} from this user database.
     *
     * @param role The role to be removed
     */
    void removeRole(WSRole role);

    /**
     * Remove the specified {@link WSUser} from this user database.
     *
     * @param user The user to be removed
     */
    void removeUser(WSUser user);
}

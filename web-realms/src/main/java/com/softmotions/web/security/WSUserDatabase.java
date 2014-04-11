package com.softmotions.web.security;

import java.io.Closeable;
import java.util.Iterator;

/**                                 [
 * Based on catalina {@link org.apache.catalina.UserDatabase}
 *
 * @author Craig R. McClanahan
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WSUserDatabase extends Closeable {

    /**
     * Return the set of {@link org.apache.catalina.Group}s defined in this user database.
     */
    public Iterator<WSGroup> getGroups();

    /**
     * Return the set of {@link org.apache.catalina.Role}s defined in this user database.
     */
    public Iterator<WSRole> getRoles();

    /**
     * Return the set of {@link org.apache.catalina.User}s defined in this user database.
     */
    public Iterator<WSUser> getUsers();

    /**
     * Create and return a new {@link WSGroup} defined in this user database.
     *
     * @param groupname   The group name of the new group (must be unique)
     * @param description The description of this group
     */
    public WSGroup createGroup(String groupname, String description);

    /**
     * Create and return a new {@link WSRole} defined in this user database.
     *
     * @param rolename    The role name of the new role (must be unique)
     * @param description The description of this role
     */
    public WSRole createRole(String rolename, String description);

    /**
     * Create and return a new {@link WSUser} defined in this user database.
     *
     * @param username The logon username of the new user (must be unique)
     * @param password The logon password of the new user
     * @param fullName The full name of the new user
     */
    public WSUser createUser(String username, String password,
                             String fullName);

    /**
     * Return the {@link WSGroup} with the specified group name, if any;
     * otherwise return <code>null</code>.
     *
     * @param groupname Name of the group to return
     */
    public WSGroup findGroup(String groupname);

    /**
     * Return the {@link WSRole} with the specified role name, if any;
     * otherwise return <code>null</code>.
     *
     * @param rolename Name of the role to return
     */
    public WSRole findRole(String rolename);

    /**
     * Return the {@link WSUser} with the specified user name, if any;
     * otherwise return <code>null</code>.
     *
     * @param username Name of the user to return
     */
    public WSUser findUser(String username);

    /**
     * Remove the specified {@link WSGroup} from this user database.
     *
     * @param group The group to be removed
     */
    public void removeGroup(WSGroup group);

    /**
     * Remove the specified {@link WSRole} from this user database.
     *
     * @param role The role to be removed
     */
    public void removeRole(WSRole role);

    /**
     * Remove the specified {@link WSUser} from this user database.
     *
     * @param user The user to be removed
     */
    public void removeUser(WSUser user);
}

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
     * Flag "can edit users (parameters)" in access mask (include creation/deletion users)
     */
    public static final int USERS_WRITABLE = 1;

    /**
     * Flag "can edit groups" in access mask (include creation/deletion groups)
     */
    public static final int GROUPS_WRITABLE = 1 << 1;

    /**
     * Flag "can edit roles" in access mask (include creation/deletion roles)
     */
    public static final int ROLES_WRITABLE = 1 << 2;

    /**
     * Flag "can edit users access" in access mask: add/remove groups and roles for users
     */
    public static final int USERS_ACCESS_WRITABLE = 1 << 3;

    /**
     * Returns <code>true</code> if can write users parameters (name, email, ...; include creation/deletion), otherwise returns <code>false</code>
     */
    public boolean isCanUsersWrite();

    /**
     * Returns <code>true</code> if can edit users access (list of roles and groups), otherwise returns <code>false</code>
     */
    public boolean isCanUsersAccessWrite();

    /**
     * Returns <code>true</code> if can write groups parameters (include creation/deletion), otherwise returns <code>false</code>
     */
    public boolean isCanGroupsWrite();

    /**
     * Returns <code>true</code> if can write roles parameters (include creation/deletion), otherwise returns <code>false</code>
     */
    public boolean isCanRolesWrite();

    /**
     * Return access mask for write operations
     */
    public int getWriteMask();


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
     * @param query         Optional query text, can be null
     * @param orderProperty Optional property name used to sort results, can be null
     * @param desc          If true use DESC sorting based on specified orderProperty otherwise ASC sorting will be used
     * @param skip          Number of records to skip
     * @param limit         Limit number of resulting records
     * @return
     */
    Iterator<WSUser> getUsers(String query, String orderProperty, boolean desc, int skip, int limit);

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

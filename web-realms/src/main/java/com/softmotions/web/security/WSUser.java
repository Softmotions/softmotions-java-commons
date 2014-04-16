package com.softmotions.web.security;

import java.security.Principal;
import java.util.Iterator;

/**
 * Based on catalina {@link org.apache.catalina.User}
 *
 * @author Craig R. McClanahan
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WSUser extends Principal {

    /**
     * Return the full name of this user.
     */
    String getFullName();

    /**
     * Set the full name of this user.
     *
     * @param fullName The new full name
     */
    void setFullName(String fullName);

    /**
     * Return the set of {@link org.apache.catalina.Group}s to which this user belongs.
     */
    Iterator<WSGroup> getGroups();

    /**
     * Return the logon password of this user, optionally prefixed with the
     * identifier of an encoding scheme surrounded by curly braces, such as
     * <code>{md5}xxxxx</code>.
     */
    String getPassword();

    /**
     * Set the logon password of this user, optionally prefixed with the
     * identifier of an encoding scheme surrounded by curly braces, such as
     * <code>{md5}xxxxx</code>.
     *
     * @param password The new logon password
     */
    void setPassword(String password);


    String getEmail();

    void setEmail(String email);

    /**
     * Return all role names
     * assigned to user.
     */
    String[] getRoleNames();

    /**
     * Return true if user
     * has any of specified role
     *
     * @param roles
     */
    boolean isHasAnyRole(String... roles);

    /**
     * Return the set of {@link org.apache.catalina.Role}s assigned specifically to this user.
     */
    Iterator<WSRole> getRoles();

    /**
     * Return the {@link org.apache.catalina.UserDatabase} within which this User is defined.
     */
    WSUserDatabase getUserDatabase();

    /* Set the logon username of this user, which must be unique within
     * the scope of a {@link WSUserDatabase}.
     *
     * @param username The new logon username
     */
    void setName(String username);

    /**
     * Add a new {@link WSGroup} to those this user belongs to.
     *
     * @param group The new group
     */
    void addGroup(WSGroup group);

    /**
     * Add a {@link WSRole} to those assigned specifically to this user.
     *
     * @param role The new role
     */
    void addRole(WSRole role);

    /**
     * Is this user in the specified {@link WSGroup}?
     *
     * @param group The group to check
     */
    boolean isInGroup(WSGroup group);

    /**
     * Is this user specifically assigned the specified {@link WSRole}?  This
     * method does <strong>NOT</strong> check for roles inherited based on
     * {@link WSGroup} membership.
     *
     * @param role The role to check
     */
    boolean isInRole(WSRole role);

    /**
     * Remove a {@link WSGroup} from those this user belongs to.
     *
     * @param group The old group
     */
    void removeGroup(WSGroup group);

    /**
     * Remove all {@link WSGroup}s from those this user belongs to.
     */
    void removeGroups();

    /**
     * Remove a {@link WSUserDatabase} from those assigned to this user.
     *
     * @param role The old role
     */
    void removeRole(WSRole role);

    /**
     * Remove all {@link WSRole}s from those assigned to this user.
     */
    void removeRoles();
}
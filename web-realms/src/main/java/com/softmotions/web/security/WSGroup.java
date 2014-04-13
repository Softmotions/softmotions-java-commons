package com.softmotions.web.security;


import java.security.Principal;
import java.util.Iterator;

/**
 * Based on catalina {@link org.apache.catalina.Group}
 *
 * @author Craig R. McClanahan
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WSGroup extends Principal {

    /**
     * Return the description of this group.
     */
    String getDescription();

    /**
     * Set the description of this group.
     *
     * @param description The new description
     */
    void setDescription(String description);

    /**
     * Set the group name of this group, which must be unique
     * within the scope of a {@link org.apache.catalina.UserDatabase}.
     *
     * @param groupname The new group name
     */
    void setName(String groupname);

    /**
     * Return the set of {@link org.apache.catalina.Role}s assigned specifically to this group.
     */
    Iterator<WSRole> getRoles();

    /**
     * Return the {@link org.apache.catalina.UserDatabase} within which this Group is defined.
     */
    WSUserDatabase getUserDatabase();

    /**
     * Return the set of {@link org.apache.catalina.User}s that are members of this group.
     */
    Iterator<WSUser> getUsers();

    /**
     * Add a new {@link WSRole} to those assigned specifically to this group.
     *
     * @param role The new role
     */
    void addRole(WSRole role);

    /**
     * Is this group specifically assigned the specified {@link WSRole}?
     *
     * @param role The role to check
     */
    boolean isInRole(WSRole role);

    /**
     * Remove a {@link WSRole} from those assigned to this group.
     *
     * @param role The old role
     */
    void removeRole(WSRole role);

    /**
     * Remove all {@link WSRole}s from those assigned to this group.
     */
    void removeRoles();
}

package com.softmotions.web.security;

import java.security.Principal;

/**
 * Based on catalina {@link org.apache.catalina.Role}
 *
 * @author Craig R. McClanahan
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public interface WSRole extends Principal {

    /**
     * Return the description of this role.
     */
    String getDescription();

    /**
     * Set the description of this role.
     *
     * @param description The new description
     */
    void setDescription(String description);

    /**
     * Set the role name of this role, which must be unique
     * within the scope of a {@link WSUserDatabase}.
     *
     * @param rolename The new role name
     */
    void setName(String rolename);

    /**
     * Return the {@link WSUserDatabase} within which this Role is defined.
     */
    WSUserDatabase getUserDatabase();
}

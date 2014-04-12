package com.softmotions.web.security;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public abstract class AbstractWSRole implements WSRole {

    /**
     * The description of this Role.
     */
    protected String description = null;

    /**
     * The role name of this Role.
     */
    protected String rolename = null;

    public String getRolename() {
        return rolename;
    }

    public void setRolename(String rolename) {
        this.rolename = rolename;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return getRolename();
    }

    protected AbstractWSRole() {
    }

    protected AbstractWSRole(String rolename, String description) {
        this.rolename = rolename;
        this.description = description;
    }


}

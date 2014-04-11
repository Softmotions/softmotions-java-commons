package com.softmotions.web.security.tomcat;

import com.softmotions.web.security.WSGroup;
import com.softmotions.web.security.WSRole;
import com.softmotions.web.security.WSUser;
import com.softmotions.web.security.WSUserDatabase;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.naming.ContextBindings;

import javax.naming.Context;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Based on {@link org.apache.catalina.realm.UserDatabaseRealm}
 *
 * @author Craig R. McClanahan
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WSUserDatabaseRealm extends RealmBase {

    /**
     * The <code>UserDatabase</code> we will use to authenticate users
     * and identify associated roles.
     */
    protected WSUserDatabase database = null;

    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String info = "com.softmotions.web.security.tomcat.WSUserDatabaseRealm/1.0";

    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String name = "WSUserDatabaseRealm";

    /**
     * The global JNDI name of the <code>UserDatabase</code> resource
     * we will be utilizing.
     */
    protected String resourceName = "WSUserDatabase";

    /**
     * Use context local database.
     */
    protected boolean localDatabase;


    public String getInfo() {
        return info;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public boolean isLocalDatabase() {
        return localDatabase;
    }

    public void setLocalDatabase(boolean localDatabase) {
        this.localDatabase = localDatabase;
    }

    /**
     * Return <code>true</code> if the specified Principal has the specified
     * security role, within the context of this Realm; otherwise return
     * <code>false</code>. This implementation returns <code>true</code>
     * if the <code>User</code> has the role, or if any <code>Group</code>
     * that the <code>User</code> is a member of has the role.
     *
     * @param principal Principal for whom the role is to be checked
     * @param role      Security role to be checked
     */
    public boolean hasRole(Wrapper wrapper, Principal principal, String role) {
        // Check for a role alias defined in a <security-role-ref> element
        if (wrapper != null) {
            String realRole = wrapper.findSecurityReference(role);
            if (realRole != null)
                role = realRole;
        }
        if (principal instanceof GenericPrincipal) {
            GenericPrincipal gp = (GenericPrincipal) principal;
            if (gp.getUserPrincipal() instanceof WSUser) {
                principal = gp.getUserPrincipal();
            }
        }
        if (!(principal instanceof WSUser)) {
            //Play nice with SSO and mixed Realms
            return super.hasRole(null, principal, role);
        }
        if ("*".equals(role)) {
            return true;
        } else if (role == null) {
            return false;
        }
        WSUser user = (WSUser) principal;
        WSRole dbrole = database.findRole(role);
        if (dbrole == null) {
            return false;
        }
        if (user.isInRole(dbrole)) {
            return true;
        }
        Iterator<WSGroup> groups = user.getGroups();
        while (groups.hasNext()) {
            WSGroup group = groups.next();
            if (group.isInRole(dbrole)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Return a short name for this Realm implementation.
     */
    protected String getName() {
        return name;
    }

    /**
     * Return the password associated with the given principal's user name.
     */
    protected String getPassword(String username) {
        WSUser user = database.findUser(username);
        if (user == null) {
            return null;
        }
        return user.getPassword();
    }

    /**
     * Return the Principal associated with the given user name.
     */
    protected Principal getPrincipal(String username) {
        WSUser user = database.findUser(username);
        if (user == null) {
            return null;
        }
        List<String> roles = new ArrayList<String>();
        Iterator<WSRole> uroles = user.getRoles();
        while (uroles.hasNext()) {
            WSRole role = uroles.next();
            roles.add(role.getName());
        }
        Iterator<WSGroup> groups = user.getGroups();
        while (groups.hasNext()) {
            WSGroup group = groups.next();
            uroles = group.getRoles();
            while (uroles.hasNext()) {
                WSRole role = uroles.next();
                roles.add(role.getName());
            }
        }
        return new GenericPrincipal(username, user.getPassword(), roles, user);
    }

    /**
     * Prepare for the beginning of active use of the public methods of this
     * component and implement the requirements of
     * {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @throws org.apache.catalina.LifecycleException if this component detects a fatal error
     *                                                that prevents this component from being used
     */
    protected void startInternal() throws LifecycleException {
        try {
            Context context = null;
            if (localDatabase) {
                context = ContextBindings.getClassLoader();
                context = (Context) context.lookup("comp/env");
            } else {
                context = getServer().getGlobalNamingContext();
            }
            database = (WSUserDatabase) context.lookup(resourceName);
        } catch (Throwable e) {
            containerLog.error(sm.getString("wsUserDatabaseRealm.lookup"), e);
            database = null;
        }
        if (database == null) {
            throw new LifecycleException(sm.getString("wsUserDatabaseRealm.noDatabase", resourceName));
        }
        super.startInternal();
    }

    /**
     * Gracefully terminate the active use of the public methods of this
     * component and implement the requirements of
     * {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
     *
     * @throws LifecycleException if this component detects a fatal error
     *                            that needs to be reported
     */
    protected void stopInternal() throws LifecycleException {
        // Perform normal superclass finalization
        super.stopInternal();
        // Release reference to our user database
        database = null;
    }
}

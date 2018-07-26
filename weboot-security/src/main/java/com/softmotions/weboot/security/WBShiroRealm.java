package com.softmotions.weboot.security;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.web.security.WSRole;
import com.softmotions.web.security.WSUser;
import com.softmotions.web.security.WSUserDatabase;

/**
 * An implementation of the {@link AuthorizingRealm Realm} interface based on {@link WSUserDatabase}
 *
 * @author Motyrev Pavel (legioner.r@gmail.com)
 */
public class WBShiroRealm extends AuthorizingRealm {

    private static final Logger log = LoggerFactory.getLogger(WBShiroRealm.class);

    private WSUserDatabase database;

    public void setDatabase(WSUserDatabase database) {
        this.database = database;
    }

    public WBShiroRealm() {
    }

    public WBShiroRealm(WSUserDatabase database) {
        this(database, null);
    }

    public WBShiroRealm(WSUserDatabase database, @Nullable CredentialsMatcher matcher) {
        log.info("A new WBShiroRealm...");
        this.database = database;
        if (matcher != null) {
            setCredentialsMatcher(matcher);
        } else {
            log.warn("No CredentialsMatcher was set!");
        }
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();
        WSUser user = database.findUser(username);
        if (user == null) {
            throw new UnknownAccountException();
        }
        if (getCredentialsMatcher() instanceof WBShiroPasswordMatcher) {
            return new SimpleAuthenticationInfo(user, user, getName());
        }
        return new SimpleAuthenticationInfo(user.getName(), user.getPassword(), getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Set<String> roleNames = new LinkedHashSet<>();
        String username = getAvailablePrincipal(principals).toString();
        WSUser user = database.findUser(username);
        if (user != null) {
            Iterator<WSRole> roles = user.getRoles();
            while (roles.hasNext()) {
                roleNames.add(roles.next().getName());
            }
        }
        return new SimpleAuthorizationInfo(roleNames);
    }
}

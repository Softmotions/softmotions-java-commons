package com.softmotions.weboot.security;

import java.security.NoSuchAlgorithmException;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.PasswordMatcher;

import com.softmotions.web.security.WSUser;

/**
 * An implementation of the {@link PasswordMatcher} based on {@link WSUser}
 *
 * @author Motyrev Pavel (legioner.r@gmail.com)
 */
public class WBShiroPasswordMatcher extends PasswordMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        Object storedCredentials = getStoredPassword(info);
        String submittedPassword = new String((char[]) getSubmittedPassword(token));
        if (storedCredentials instanceof WSUser) {
            WSUser user = (WSUser) storedCredentials;
            try {
                return user.matchPassword(submittedPassword);
            } catch (NoSuchAlgorithmException ignored) {
                return false;
            }
        }
        // fallback to plain text password matching
        String storedPassword = (String) storedCredentials;
        return storedPassword.equals(submittedPassword);
    }
}

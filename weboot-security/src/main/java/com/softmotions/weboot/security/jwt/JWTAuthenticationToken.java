package com.softmotions.weboot.security.jwt;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.shiro.authc.AuthenticationToken;

/**
 * JWT authentication token
 *
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class JWTAuthenticationToken implements AuthenticationToken {

    private final String token;

    public JWTAuthenticationToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("token", token)
                .toString();
    }
}

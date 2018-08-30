package com.softmotions.weboot.security.jwt;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

/**
 * @author Adamansky Anton (adamansky@softmotions.com)
 */
public class JWTAuthenticationFilter extends AuthenticatingFilter {

    @Override
    protected AuthenticationToken createToken(ServletRequest sreq, ServletResponse sresp) throws Exception {
        HttpServletRequest req = WebUtils.toHttp(sreq);
        String jwt = req.getHeader("Authorization");
        if (jwt == null) throw new AuthenticationException("Missing 'Authorization' HTTP header");
        jwt = jwt.substring(jwt.indexOf(' ')).trim();
        return new JWTAuthenticationToken(jwt);
    }

    @Override
    protected boolean onAccessDenied(ServletRequest sreq, ServletResponse sresp) throws Exception {
        HttpServletRequest req = WebUtils.toHttp(sreq);
        HttpServletResponse resp = WebUtils.toHttp(sresp);
        boolean loggedIn = false;
        String jwt = req.getHeader("Authorization");
        if (jwt != null && jwt.startsWith("Bearer ")) {
            loggedIn = executeLogin(sreq, sresp);
        }
        if (!loggedIn) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
        return loggedIn;
    }
}

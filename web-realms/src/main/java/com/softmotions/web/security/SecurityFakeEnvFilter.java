package com.softmotions.web.security;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.security.Principal;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class SecurityFakeEnvFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityFakeEnvFilter.class);

    private WSUserDatabase db;

    private String username;

    @Override
    public void init(FilterConfig conf) throws ServletException {
        String dbJndiName = conf.getInitParameter("dbJndiName");
        if (!StringUtils.isBlank(dbJndiName)) {
            try {
                InitialContext ctx = new InitialContext();
                db = (WSUserDatabase) ctx.lookup(dbJndiName);
            } catch (NamingException e) {
                log.error("", e);
                throw new ServletException(e);
            }
        }
        username = conf.getInitParameter("username");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (db != null && username != null) {
            request = new SecurityHttpServletRequestWrapper(db, username, (HttpServletRequest) request);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        this.db = null;
        this.username = null;
    }

    private static class SecurityHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private final WSUserDatabase db;

        private final String username;

        private SecurityHttpServletRequestWrapper(WSUserDatabase db, final String username,
                                                  HttpServletRequest request) {
            super(request);
            this.db = db;
            this.username = username;
        }

        @Override
        public String getRemoteUser() {
            return username;
        }

        @Override
        public Principal getUserPrincipal() {
            return db.findUser(username);
        }

        @Override
        public boolean isUserInRole(String role) {
            WSUser wsuser = db.findUser(username);
            WSRole wsrole = db.findRole(role);
            return (wsuser != null && wsrole != null && wsuser.isInRole(wsrole));
        }
    }
}

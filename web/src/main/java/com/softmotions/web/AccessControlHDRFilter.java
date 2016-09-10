package com.softmotions.web;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;

/**
 * Разрешает все Access-Control-* заголовки.
 *
 * @author Adamansky Anton (adamansky@gmail.com)
 * @version $Id$
 */
public class AccessControlHDRFilter implements Filter {

    private String value;

    private boolean enabled;

    private String exposeHeaders;


    @Override
    public void init(FilterConfig config) throws ServletException {
        value = config.getInitParameter("headerValue");
        if (config.getInitParameter("enabled") != null) {
            enabled = BooleanUtils.toBoolean(config.getInitParameter("enabled"));
        }
        if (config.getInitParameter("disabled") != null) {
            enabled = !BooleanUtils.toBoolean(config.getInitParameter("disabled"));
        }
        if (config.getInitParameter("exposeHeaders") != null) {
            exposeHeaders = config.getInitParameter("exposeHeaders");
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        if (enabled && value != null) {
            HttpServletRequest hreq = (HttpServletRequest) req;
            HttpServletResponse hresp = (HttpServletResponse) resp;
            hresp.setHeader("Access-Control-Allow-Origin", value);

            String origin = hreq.getHeader("Origin");
            String rheaders = hreq.getHeader("Access-Control-Request-Headers");
            if (rheaders != null) {
                hresp.setHeader("Access-Control-Allow-Headers", rheaders);
            }
            String rmethod = hreq.getHeader("Access-Control-Request-Method");
            if (rmethod != null) {
                hresp.setHeader("Access-Control-Allow-Methods", rmethod);
            }
            if (exposeHeaders != null && (rheaders != null || rmethod != null || origin != null)) {
                hresp.setHeader("Access-Control-Expose-Headers", exposeHeaders);
            }
        }
        chain.doFilter(req, resp);
    }


    @Override
    public void destroy() {

    }
}


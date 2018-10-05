package com.softmotions.weboot.security;

import javax.servlet.ServletContext;

import org.apache.shiro.web.env.IniWebEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.weboot.WBServletListener;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBShiroWebEnvironment extends IniWebEnvironment {

    private static final Logger log = LoggerFactory.getLogger(WBShiroWebEnvironment.class);

    @Override
    public void setServletContext(ServletContext sctx) {
        super.setServletContext(sctx);
        ServicesConfiguration env = (ServicesConfiguration) sctx.getAttribute(WBServletListener.WEBOOT_CFG_SCTX_KEY);
        String configLocations = env.xcfg().textPattern("security.shiro-config-locations", "/WEB-INF/shiro.ini");
        log.info("Shiro config locations: {}", configLocations);
        setConfigLocations(configLocations);
    }

}

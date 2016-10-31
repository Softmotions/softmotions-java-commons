package com.softmotions.weboot.security;

import javax.servlet.ServletContext;

import org.apache.shiro.web.env.IniWebEnvironment;

import com.softmotions.commons.ServicesConfiguration;
import com.softmotions.weboot.WBServletListener;

/**
 * @author Adamansky Anton (adamansky@gmail.com)
 */
public class WBShiroWebEnvironment extends IniWebEnvironment {

    @Override
    public void setServletContext(ServletContext sctx) {
        super.setServletContext(sctx);
        ServicesConfiguration env = (ServicesConfiguration) sctx.getAttribute(WBServletListener.WEBOOT_CFG_SCTX_KEY);
        String configLocations = env.xcfg().getString("security.shiro-config-locations", "/WEB-INF/shiro.ini");
        setConfigLocations(configLocations);
    }

}
